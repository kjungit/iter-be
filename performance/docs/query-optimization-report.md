# 쿼리·인덱스 최적화 보고서

## 1. 적용 범위

| 대상 | 적용 내용 |
|---|---|
| 장비 조회 | Review 조인·평균·건수 집계·`GROUP BY` 제거 |
| 받은 대여 요청 | 페이지별 회원·결제 상태 일괄 조회로 N+1 제거 |
| 관리자 목록 5개 | `(createdAt, id)` keyset cursor, count 쿼리 제거 |
| 존재 여부 확인 | 전체 `COUNT` 대신 ID 최대 1건 조회 |
| 알림 목록 | `createdAt DESC, id DESC`로 정렬 안정화 |
| 주요 조회 | 정렬·필터 조건에 맞춰 엔티티 `@Index` 선언 |

리뷰 기능은 비활성화했지만 기존 장비 응답 계약은 유지한다.

- `averageRating`: `0.0`
- `reviewCount`: `0`
- `RATING_DESC`: `createdAt DESC, id DESC`로 처리

## 2. 관리자 커서 조회

적용 API:

- `/api/v1/admin/users`
- `/api/v1/admin/equipment`
- `/api/v1/admin/reports`
- `/api/v1/admin/payments`
- `/api/v1/admin/actions`

모두 `createdAt DESC, id DESC`로 정렬하고 아래 조건으로 다음 데이터를 조회한다.

```sql
created_at < :cursorCreatedAt
OR (created_at = :cursorCreatedAt AND id < :cursorId)
```

`size + 1`건을 조회해 `hasNext`를 판단하므로 전체 count 쿼리가 없다.

- 첫 요청: `cursor`를 생략하고 `size`만 전달한다.
- 다음 요청: 응답의 `nextCursor`와 동일한 검색 조건을 전달한다.
- 응답: `content`, `nextCursor`, `hasNext`, `size`를 반환한다.
- 마지막 페이지: `hasNext=false`, `nextCursor=null`을 반환한다.
- 제한: 전체 페이지 수와 임의 페이지 이동은 지원하지 않는다.

## 3. 적용 인덱스

인덱스는 엔티티의 `@Table(indexes = ...)`에 선언했다.

| 테이블 | 인덱스 컬럼 | 주요 대상 |
|---|---|---|
| users | `created_at DESC, id DESC` | 관리자 회원 전체 목록 |
| users | `status, created_at DESC, id DESC` | 회원 상태 필터 |
| equipment | `created_at DESC, id DESC` | 관리자 장비 전체 목록 |
| equipment | `status, created_at DESC, id DESC` | 상태 필터·공개 최신순 |
| equipment | `owner_id, created_at DESC, id DESC` | 내 장비 |
| rental | `renter_id, created_at DESC, id DESC` | 빌린 이력 |
| rental | `equipment_id, created_at DESC, id DESC` | 빌려준 이력·받은 요청 |
| rental | `equipment_id, start_date, end_date, id, status` | 기간 충돌·일정 |
| rental | `status, created_at` | 미결제 만료 |
| payment | `created_at DESC, id DESC` | 관리자 결제 전체 목록 |
| payment | `status, created_at DESC, id DESC` | 결제 상태 필터 |
| report | `created_at DESC, id DESC` | 관리자 신고 전체 목록 |
| report | `status, target_type, created_at DESC, id DESC` | 관리자 신고 필터 |
| report | `reporter_id, created_at DESC, id DESC` | 내 신고 목록 |
| report | `reporter_id, target_type, target_id, status` | 처리 중 중복 신고 |
| notification | `receiver_id, created_at DESC, id DESC` | 알림 목록 |
| notification | `receiver_id, is_read, created_at DESC, id DESC` | 미읽음 목록·건수 |
| admin_action | `created_at DESC, id DESC` | 처리 이력 전체 목록 |
| admin_action | `target_type, target_id, action, created_at DESC, id DESC` | 처리 이력 필터 |

이미 Unique 인덱스가 있는 `payment.rental_id`, `payment.order_id`, `receipt.rental_id`, `return_receipt.rental_id`에는 중복 인덱스를 추가하지 않았다.

`ddl-auto=create/update`에서는 Hibernate가 `@Index`를 반영할 수 있다. `ddl-auto=validate/none`은 기존 DB를 변경하지 않으므로 별도 DB 마이그레이션이 필요하다.

## 4. EXPLAIN ANALYZE 결과

환경: MySQL 9.1, 회원 1,000건, 장비 5,000건, 대여 20,000건, 결제 10,000건, 신고 약 5,000건.

| 대상 | 변경 전 | 변경 후 |
|---|---:|---:|
| 공개 최신순 장비 | 5.04ms | 0.217ms |
| 기간 조건 장비 검색 | 12.1ms | 0.471ms |
| 빌린 이력 목록 | 12.0ms | 0.066ms |
| 빌려준 이력 목록 | 35.1ms | 1.05ms |
| 대여 충돌 count/첫 행 | 4.85ms | 0.0083ms |
| 내 신고 목록 | 2.11ms | 0.113ms |
| 중복 신고 첫 행 | 0.0578ms | 0.0083ms |
| 전체 알림 목록 | 1.79ms | 0.0868ms |
| 관리자 결제 목록 | 17.9ms | 0.336ms |

이 값은 Repository 구조를 대응시킨 로컬 MySQL SQL을 한 번씩 실행한 상대 비교값이다. 운영 응답시간이나 k6 결과가 아니며, 최종 판단은 Hibernate가 생성한 실제 SQL의 실행 계획과 동일 환경의 k6 전후 측정으로 한다.

리뷰 집계 쿼리에 인덱스만 추가했을 때는 오히려 느려졌다. 따라서 인덱스만 추가하지 않고 사용하지 않는 Review 조인과 집계를 제거했다.
