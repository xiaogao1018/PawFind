
package com.pawfind.backend.controller;

import com.pawfind.backend.common.exception.InvalidCredentialsException;
import com.pawfind.backend.common.result.Result;

import com.pawfind.backend.dto.PetLostCreateDTO;
import com.pawfind.backend.dto.PetLostUpdateDTO;

import com.pawfind.backend.service.PetLostService;

import com.pawfind.backend.vo.PetLostDetailVO;
import com.pawfind.backend.vo.PetLostPageVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pet-lost")
public class PetLostController {

    private final PetLostService petLostService;

    public PetLostController(PetLostService petLostService) {
        this.petLostService = petLostService;
    }

    // 获取当前登录用户 ID
    private Long getCurrentUserId(HttpServletRequest request) {

        Long currentUserId =
                (Long) request.getAttribute("currentUserId");

        if (currentUserId == null) {
            throw new InvalidCredentialsException("请先登录");
        }

        return currentUserId;
    }

    // =========================
    // W3D1：发布走失信息
    // =========================

    @PostMapping
    public ResponseEntity<Result<PetLostDetailVO>> createLostPet(
            @Valid @RequestBody PetLostCreateDTO dto,
            HttpServletRequest request) {

        Long currentUserId = getCurrentUserId(request);

        PetLostDetailVO result =
                petLostService.createLostPet(dto, currentUserId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Result.success(result));
    }

    // =========================
    // W3D1：查询走失详情
    // =========================

    @GetMapping("/{id}")
    public Result<PetLostDetailVO> getLostPetById(
            @PathVariable Long id) {

        return Result.success(
                petLostService.getLostPetById(id)
        );
    }

    // =========================
    // W3D2：编辑走失信息
    // =========================

    @PutMapping("/{id}")
    public Result<PetLostDetailVO> updateLostPet(
            @PathVariable Long id,
            @Valid @RequestBody PetLostUpdateDTO dto,
            HttpServletRequest request) {

        Long currentUserId = getCurrentUserId(request);

        return Result.success(
                petLostService.updateLostPet(
                        id, dto, currentUserId
                )
        );
    }

    // =========================
    // W3D2：撤回走失信息
    // =========================

    @DeleteMapping("/{id}")
    public Result<PetLostDetailVO> withdrawLostPet(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long currentUserId = getCurrentUserId(request);

        return Result.success(
                petLostService.withdrawLostPet(
                        id, currentUserId
                )
        );
    }

    // =========================
    // W3D2：标记宠物已找到
    // =========================

    @PatchMapping("/{id}/found")
    public Result<PetLostDetailVO> markAsFound(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long currentUserId = getCurrentUserId(request);

        return Result.success(
                petLostService.markAsFound(
                        id, currentUserId
                )
        );
    }

    // =========================
    // W3D2：分页查询走失信息
    // =========================

    @GetMapping
    public Result<PetLostPageVO> getLostPetPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        return Result.success(
                petLostService.getLostPetPage(page, size)
        );
    }
}
