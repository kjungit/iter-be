package com.example.iter.device.controller.api;

import com.example.iter.common.dto.response.PageResponse;
import com.example.iter.common.security.CustomUserDetails;
import com.example.iter.device.controller.api.spec.MyEquipmentApiSpec;
import com.example.iter.device.dto.request.MyEquipmentSearchRequest;
import com.example.iter.device.dto.response.MyEquipmentSummaryResponse;
import com.example.iter.device.service.EquipmentQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/devices")
@RequiredArgsConstructor
public class MyEquipmentApiController implements MyEquipmentApiSpec {

    private final EquipmentQueryService equipmentQueryService;

    @Override
    @GetMapping
    public ResponseEntity<PageResponse<MyEquipmentSummaryResponse>> getMyEquipment(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute MyEquipmentSearchRequest request
    ) {
        return ResponseEntity.ok(equipmentQueryService.getMyEquipment(
                principal.getUser().getId(), request));
    }
}
