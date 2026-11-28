package model.mapper.typehandler;

import org.apache.ibatis.type.TypeHandlerRegistry;

import model.domain.enums.ApprovalStatus;
import model.domain.enums.BookingStatus;
import model.domain.enums.PaymentStatus;
import model.domain.enums.QueueStatus;
import model.domain.enums.ScheduleStatus;
import model.domain.enums.SeatStatus;
import model.domain.enums.SendStatus;

/** MyBatisConfig와 테스트가 같은 등록 목록을 쓰도록 한 곳에 모아 둔다. */
public final class TypeHandlers {

    private TypeHandlers() {
    }

    public static void registerAll(TypeHandlerRegistry registry) {
        registry.register(ApprovalStatus.class, CodeEnumTypeHandler.class);
        registry.register(BookingStatus.class, CodeEnumTypeHandler.class);
        registry.register(PaymentStatus.class, CodeEnumTypeHandler.class);
        registry.register(QueueStatus.class, CodeEnumTypeHandler.class);
        registry.register(ScheduleStatus.class, CodeEnumTypeHandler.class);
        registry.register(SeatStatus.class, CodeEnumTypeHandler.class);
        registry.register(SendStatus.class, CodeEnumTypeHandler.class);

        // Oracle에는 boolean 컬럼이 없고 우리 boolean 컬럼은 전부 Y/N이라 boolean 전체에 적용한다
        registry.register(Boolean.class, YesNoBooleanTypeHandler.class);
        registry.register(boolean.class, YesNoBooleanTypeHandler.class);
    }
}
