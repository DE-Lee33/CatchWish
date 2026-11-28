package model.mapper.typehandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.junit.Test;

import model.domain.enums.BookingStatus;
import model.domain.enums.SeatStatus;

public class TypeHandlersTest {

    private final CodeEnumTypeHandler<BookingStatus> enumHandler = new CodeEnumTypeHandler<>(BookingStatus.class);
    private final YesNoBooleanTypeHandler booleanHandler = new YesNoBooleanTypeHandler();

    // 실제 DB 없이 getString만 흉내 내는 ResultSet
    private static ResultSet resultSetReturning(String value) {
        return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class<?>[] { ResultSet.class },
                (proxy, method, args) -> "getString".equals(method.getName()) ? value : null);
    }

    // setString으로 들어온 값을 기록하는 PreparedStatement
    private static PreparedStatement statementCapturing(AtomicReference<String> captured) {
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                new Class<?>[] { PreparedStatement.class }, (proxy, method, args) -> {
                    if ("setString".equals(method.getName())) {
                        captured.set((String) args[1]);
                    }
                    return null;
                });
    }

    @Test
    public void 코드를_enum으로_읽는다() throws Exception {
        assertEquals(BookingStatus.CONFIRMED, enumHandler.getResult(resultSetReturning("C"), "BOOKING_STATUS"));
    }

    @Test
    public void NULL은_null로_읽는다() throws Exception {
        assertNull(enumHandler.getResult(resultSetReturning(null), "BOOKING_STATUS"));
    }

    @Test
    public void 잘못된_코드는_예외() {
        assertThrows(IllegalArgumentException.class,
                () -> enumHandler.getNullableResult(resultSetReturning("Z"), "BOOKING_STATUS"));
    }

    @Test
    public void enum을_1글자_코드로_저장한다() throws Exception {
        AtomicReference<String> captured = new AtomicReference<>();
        enumHandler.setParameter(statementCapturing(captured), 1, BookingStatus.CANCELED, JdbcType.CHAR);
        assertEquals("X", captured.get());
    }

    @Test
    public void YN을_boolean으로_읽고_쓴다() throws Exception {
        assertEquals(Boolean.TRUE, booleanHandler.getResult(resultSetReturning("Y"), "IS_READ"));
        assertEquals(Boolean.FALSE, booleanHandler.getResult(resultSetReturning("N"), "IS_READ"));
        assertNull(booleanHandler.getResult(resultSetReturning(null), "IS_READ"));

        AtomicReference<String> captured = new AtomicReference<>();
        booleanHandler.setParameter(statementCapturing(captured), 1, true, JdbcType.CHAR);
        assertEquals("Y", captured.get());
    }
    
    @Test
    public void YN이_아닌_코드는_예외() {
        assertThrows(IllegalArgumentException.class,
                () -> booleanHandler.getNullableResult(resultSetReturning("X"), "IS_READ"));
    }

    @Test
    public void 설정에_등록하면_타입별로_핸들러가_잡힌다() {
        Configuration config = new Configuration();
        TypeHandlers.registerAll(config.getTypeHandlerRegistry());

        assertTrue(config.getTypeHandlerRegistry().getTypeHandler(SeatStatus.class) instanceof CodeEnumTypeHandler);
        assertTrue(config.getTypeHandlerRegistry().getTypeHandler(Boolean.class) instanceof YesNoBooleanTypeHandler);
    }
}
