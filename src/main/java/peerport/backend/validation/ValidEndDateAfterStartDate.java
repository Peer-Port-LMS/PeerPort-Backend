package peerport.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EndDateAfterStartDate.class)
@Documented
public @interface ValidEndDateAfterStartDate {
    String message() default "End date must be after start date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    // Parameters
    String startDate() default "startDate";
    String endDate() default "endDate";
}
