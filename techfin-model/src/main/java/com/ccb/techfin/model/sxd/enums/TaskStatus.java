package com.ccb.techfin.model.sxd.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 任务状态枚举。
 *
 * @author qiuhaoquan
 * @since 2026-07-23
 */
public enum TaskStatus implements IEnum<String> {
    UNFINISHED("0"),
    COMPLETED("1");

    private final String description;

    TaskStatus(String description) {
        this.description = description;
    }

    @Override
    public String getValue() {
        return name();
    }

    public String getDescription() {
        return description;
    }
}
