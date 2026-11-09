
package com.pawfind.backend.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PetLostPageVO {

    // 当前页码
    private Integer page;

    // 每页数量
    private Integer size;

    // 总记录数
    private Long total;

    // 总页数
    private Long totalPages;

    // 当前页的走失信息
    private List<PetLostDetailVO> records;
}
