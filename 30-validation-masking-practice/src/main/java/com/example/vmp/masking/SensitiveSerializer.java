package com.example.vmp.masking;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * 敏感字段序列化器：根据字段上的 @Sensitive 注解选择打码策略。
 */
public class SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private final SensitiveType type;

    public SensitiveSerializer() {
        this(null);
    }

    public SensitiveSerializer(SensitiveType type) {
        this.type = type;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || type == null) {
            gen.writeString(value);
            return;
        }
        switch (type) {
            case PHONE:
                gen.writeString(MaskingUtil.maskPhone(value));
                break;
            case ID_CARD:
                gen.writeString(MaskingUtil.maskIdCard(value));
                break;
            case EMAIL:
                gen.writeString(MaskingUtil.maskEmail(value));
                break;
            case NAME:
                gen.writeString(MaskingUtil.maskName(value));
                break;
            case ADDRESS:
                gen.writeString(MaskingUtil.maskAddress(value));
                break;
            default:
                gen.writeString(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property == null) {
            return this;
        }
        Sensitive annotation = property.getAnnotation(Sensitive.class);
        if (annotation == null) {
            annotation = property.getContextAnnotation(Sensitive.class);
        }
        if (annotation == null) {
            return this;
        }
        return new SensitiveSerializer(annotation.value());
    }
}
