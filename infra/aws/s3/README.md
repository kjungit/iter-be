# ITer 장비 이미지 S3 설정

이 템플릿은 다음 리소스를 준비합니다.

- SSE-S3 기본 암호화를 사용하는 장비 이미지 버킷
- `equipment/temp/` 객체를 1일 후 삭제하는 Lifecycle 규칙
- 지정한 프론트 Origin만 허용하는 Presigned PUT CORS 규칙
- `equipment/public/` 최종 이미지만 공개 읽기를 허용하는 버킷 정책
- 백엔드가 업로드·검증·승격·삭제에 사용하는 최소 권한 IAM Managed Policy

## 적용 전 필요한 값

- 전 세계에서 고유한 버킷 이름
- 로컬 및 배포 프론트엔드의 정확한 Origin
- 생성된 Managed Policy를 연결할 백엔드 IAM Role 또는 로컬 개발용 IAM 사용자

## 로컬 실행

공통 개발 버킷 설정은 `application-local.yml`에 들어 있으므로 팀원이 S3 버킷명과 URL을 매번 입력할 필요는 없습니다.
각 개발자는 최소 권한 IAM 자격증명을 자신의 AWS CLI 프로필에 한 번만 등록합니다.

```bash
aws configure --profile iter-local
```

터미널에서 실행할 때는 해당 프로필만 선택합니다.

```bash
export AWS_PROFILE=iter-local
./gradlew bootRun
```

IntelliJ에서는 Run Configuration의 환경변수에 `AWS_PROFILE=iter-local`을 한 번 등록합니다.
Access Key와 Secret Access Key는 프로젝트 설정 파일에 기록하거나 Git에 커밋하지 않습니다.

## 배포 예시

AWS CLI 로그인 후 서울 리전에 배포합니다.

```bash
aws cloudformation deploy \
  --stack-name iter-equipment-images-dev \
  --template-file infra/aws/s3/template.yaml \
  --region ap-northeast-2 \
  --profile iter-infra \
  --capabilities CAPABILITY_IAM \
  --parameter-overrides \
    BucketName=YOUR_GLOBALLY_UNIQUE_BUCKET \
    'AllowedOrigins=http://localhost:5173,http://localhost:8080'
```

배포 출력의 `BackendPolicyArn`을 백엔드 실행 IAM 주체에 연결합니다. 배포 환경에서는 다음 환경변수를 설정합니다.

```text
AWS_REGION=ap-northeast-2
S3_BUCKET=<BucketName 출력값>
S3_PUBLIC_BASE_URL=<PublicBaseUrl 출력값>
S3_KEY_PREFIX=equipment
S3_PRESIGNED_URL_VALIDITY=5m
```

운영 프론트 Origin이 정해지면 `AllowedOrigins`에 정확한 HTTPS Origin을 추가해 스택을 다시 배포합니다. CloudFront와 OAC를 도입할 때는 공개 읽기 버킷 정책을 제거하고 모든 S3 Block Public Access 설정을 활성화한 뒤 `S3_PUBLIC_BASE_URL`만 CloudFront 주소로 교체합니다.

AWS 계정 또는 Organizations 수준에서 S3 Block Public Access가 강제되어 있으면 최종 이미지 공개 정책을 만들 수 없습니다. 이 경우 계정 정책을 임의로 해제하지 말고 CloudFront OAC를 먼저 적용합니다.
