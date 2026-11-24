package common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** API는 로그인한 회원만 호출할 수 있다. 클래스에 붙이면 모든 메서드에 적용됨. */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequired {
    boolean adminOnly() default false;
}