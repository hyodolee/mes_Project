package com.mes.interfaces.api.equipment;

import com.mes.application.service.equipment.EquipmentService;
import com.mes.domain.equipment.master.dto.EquipmentOptionDto;
import com.mes.domain.equipment.master.dto.WorkerOptionDto;
import com.mes.domain.equipment.master.dto.WorkcenterOptionDto;
import com.mes.domain.equipment.oper.dto.OperStatusDto;
import com.mes.domain.equipment.oper.dto.OperStatusRequest;
import com.mes.domain.equipment.downtime.dto.DowntimeDto;
import com.mes.domain.equipment.downtime.dto.DowntimeRequest;
import com.mes.domain.equipment.maint.dto.MaintHisDto;
import com.mes.domain.equipment.maint.dto.MaintHisRequest;
import com.mes.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/equipment")
public class EquipmentApiController {

    private final EquipmentService equipmentService;

    public EquipmentApiController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping("/workcenters")
    public ApiResponse<List<WorkcenterOptionDto>> getWorkcenters(
            @RequestParam(name = "plantCd", required = false) String plantCd
    ) {
        return ApiResponse.ok(equipmentService.getWorkcenters(plantCd));
    }

    @GetMapping("/options")
    public ApiResponse<List<EquipmentOptionDto>> getEquipmentOptions(
            @RequestParam(name = "plantCd", required = false) String plantCd,
            @RequestParam(name = "workcenterCd", required = false) String workcenterCd
    ) {
        return ApiResponse.ok(equipmentService.getEquipmentOptions(plantCd, workcenterCd));
    }

    @GetMapping("/workers")
    public ApiResponse<List<WorkerOptionDto>> getWorkerOptions(
            @RequestParam(name = "plantCd", required = false) String plantCd
    ) {
        return ApiResponse.ok(equipmentService.getWorkerOptions(plantCd));
    }

    @GetMapping("/oper-statuses")
    public ApiResponse<List<OperStatusDto>> getOperStatuses(
            @RequestParam(name = "plantCd", required = false) String plantCd,
            @RequestParam(name = "equipmentCd", required = false) String equipmentCd,
            @RequestParam(name = "fromDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDt,
            @RequestParam(name = "toDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDt
    ) {
        return ApiResponse.ok(equipmentService.getOperStatuses(plantCd, equipmentCd, fromDt, toDt));
    }

    @PostMapping("/oper-statuses")
    public ApiResponse<Void> recordOperStatus(@Valid @RequestBody OperStatusRequest request) {
        equipmentService.recordOperStatus(request);
        return ApiResponse.ok();
    }

    @GetMapping("/downtimes")
    public ApiResponse<List<DowntimeDto>> getDowntimes(
            @RequestParam(name = "plantCd", required = false) String plantCd,
            @RequestParam(name = "equipmentCd", required = false) String equipmentCd,
            @RequestParam(name = "fromDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDt,
            @RequestParam(name = "toDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDt
    ) {
        return ApiResponse.ok(equipmentService.getDowntimes(plantCd, equipmentCd, fromDt, toDt));
    }

    @PostMapping("/downtimes")
    public ApiResponse<Void> recordDowntime(@Valid @RequestBody DowntimeRequest request) {
        equipmentService.recordDowntime(request);
        return ApiResponse.ok();
    }

    @GetMapping("/maint-histories")
    public ApiResponse<List<MaintHisDto>> getMaintHistories(
            @RequestParam(name = "plantCd", required = false) String plantCd,
            @RequestParam(name = "equipmentCd", required = false) String equipmentCd,
            @RequestParam(name = "fromDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDt,
            @RequestParam(name = "toDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDt
    ) {
        return ApiResponse.ok(equipmentService.getMaintHistories(plantCd, equipmentCd, fromDt, toDt));
    }

    @PostMapping("/maint-histories")
    public ApiResponse<Void> recordMaintHistory(@Valid @RequestBody MaintHisRequest request) {
        equipmentService.recordMaintHistory(request);
        return ApiResponse.ok();
    }
}
