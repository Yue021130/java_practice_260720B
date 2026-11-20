package com.example.epc.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * 用户保存入参：新增 / 更新共用。
 *
 * <p>password 可空——新增时不传默认 123456，更新时为空表示不修改密码；
 * id 可空——新增为空，更新必传。</p>
 */
@Data
public class UserSaveDTO implements Serializable {

    /** 更新时必传，新增为空 */
    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 明文密码；新增为空默认 123456，更新为空表示不修改 */
    private String password;

    private String nickname;

    private Long deptId;

    /** 分配的角色 id 集合，全量覆盖 */
    private List<Long> roleIds;
}
