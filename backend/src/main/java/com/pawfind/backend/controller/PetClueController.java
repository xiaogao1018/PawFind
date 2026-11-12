package com.pawfind.backend.controller;

import com.pawfind.backend.common.result.Result;
import com.pawfind.backend.entity.PetClue;
import com.pawfind.backend.service.PetClueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PetClueController {
    private final PetClueService service;

    public PetClueController(PetClueService service) {
        this.service = service;
    }

    @GetMapping("/pet-lost/{id}/clues")
    public Result<List<PetClue>> list(@PathVariable long id,
                                      @RequestAttribute(required = false) Long currentUserId,
                                      @RequestAttribute(required = false) String currentUserRole) {
        return Result.success(service.list(id, currentUserId, currentUserRole));
    }

    @PostMapping("/pet-lost/{id}/clues")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<PetClue> create(@PathVariable long id, @Valid @RequestBody CreateClue dto,
                                  @RequestAttribute Long currentUserId) {
        return Result.success(service.create(id, dto.content(), dto.contactInfo(), dto.sightingTime(),
                dto.sightingLocation(), currentUserId));
    }

    @PatchMapping("/pet-clues/{id}/status")
    public Result<PetClue> updateStatus(@PathVariable long id, @Valid @RequestBody ClueStatus dto,
                                        @RequestAttribute Long currentUserId) {
        return Result.success(service.updateStatus(id, dto.status(), currentUserId));
    }

    public record CreateClue(
            @NotBlank(message = "请填写线索内容")
            @Size(max = 2000, message = "线索内容不能超过2000个字符") String content,
            @Size(max = 100, message = "联系方式不能超过100个字符") String contactInfo,
            @PastOrPresent(message = "目击时间不能晚于当前时间") LocalDateTime sightingTime,
            @Size(max = 255, message = "目击地点不能超过255个字符") String sightingLocation) {
    }

    public record ClueStatus(
            @NotBlank(message = "请填写线索状态")
            @Pattern(regexp = "PENDING|VERIFIED|INVALID", message = "线索状态不合法") String status) {
    }
}
