package model.mapper.typehandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * DB의 1글자 코드(CHAR(1)) ↔ 상태 enum 변환.
 * model.domain.enums의 enum들이 가진 getCode() / static fromCode(String)을 그대로 사용
 */
public class CodeEnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

    private final Class<E> type;
    private final Method getCode;
    private final Method fromCode;

    // MyBatis가 register(enum 클래스, 이 핸들러)로 등록할 때 enum 클래스를 넘겨줌
    public CodeEnumTypeHandler(Class<E> type) {
        if (type == null) {
            throw new IllegalArgumentException("enum 타입이 필요합니다.");
        }
        this.type = type;
        try {
            this.getCode = type.getMethod("getCode");
            this.fromCode = type.getMethod("fromCode", String.class);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(type.getName() + "에 getCode()와 fromCode(String)이 필요합니다.", ex);
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, (String) invoke(getCode, parameter));
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toEnum(rs.getString(columnName));
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toEnum(rs.getString(columnIndex));
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toEnum(cs.getString(columnIndex));
    }

    private E toEnum(String code) {
        return (code == null) ? null : type.cast(invoke(fromCode, null, code));
    }

    private static Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof RuntimeException runtime) {
                throw runtime; // 잘못된 코드면 fromCode의 IllegalArgumentException
            }
            throw new IllegalStateException(ex);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
