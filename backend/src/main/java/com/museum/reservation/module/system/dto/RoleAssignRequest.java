package com.museum.reservation.module.system.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RoleAssignRequest {

    @NotEmpty(message = "请至少选择一个角色")
    private List<String> roles;
}
