package com.coiflow.controller.salon;

import com.coiflow.dto.salon.CreateSalonRequest;
import com.coiflow.dto.salon.SalonResponse;
import com.coiflow.dto.salon.UpdateSalonRequest;
import com.coiflow.service.salon.SalonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/salons")
@RequiredArgsConstructor
public class SalonController {

    private final SalonService salonService;

    @PostMapping
    public ResponseEntity<SalonResponse> create(@Valid @RequestBody CreateSalonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salonService.createSalon(request));
    }

    @GetMapping
    public ResponseEntity<List<SalonResponse>> getAll() {
        return ResponseEntity.ok(salonService.getAllSalons());
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalonResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateSalonRequest request) {
        return ResponseEntity.ok(salonService.updateSalon(id, request));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<SalonResponse> toggle(@PathVariable String id) {
        return ResponseEntity.ok(salonService.toggleActive(id));
    }
}
