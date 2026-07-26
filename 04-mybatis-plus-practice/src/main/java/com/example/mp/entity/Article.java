package com.example.mp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文章实体：演示 TypeHandler + ActiveRecord 模式。
 *
 * 面试八股：
 * - extends Model<T> 后可直接调用 article.insert() / article.selectById()，即 ActiveRecord 模式
 * - @TableField(typeHandler = JacksonTypeHandler.class) 把 Map/List 等对象序列化为 JSON 字符串存库
 * - 使用 typeHandler 时建议开启 autoResultMap = true，否则查询结果可能无法正确反序列化
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "t_article", autoResultMap = true)
public class Article extends Model<Article> {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String title;

    private String content;

    /**
     * extra 以 JSON 字符串形式存储到数据库，读取时自动转回 Map。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extra;

    private LocalDateTime createTime;

    @Override
    public Serializable pkVal() {
        return this.id;
    }
}
