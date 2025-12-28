package peerport.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Method;
import java.util.Date;

public class EndDateAfterStartDate implements ConstraintValidator<ValidEndDateAfterStartDate, Object> {

    private String startDate;
    private String endDate;
    
    @Override
    public void initialize(ValidEndDateAfterStartDate constraintAnnotation) {
        this.startDate = constraintAnnotation.startDate();
        this.endDate = constraintAnnotation.endDate();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        // Allow null values (use @NotNull separately if required)
        if (obj == null) {
            return true;
        }

        try {
            // Build getter method names
            String startMethod = "get" + startDate.substring(0,1).toUpperCase() + startDate.substring(1);
            String endMethod = "get" + endDate.substring(0,1).toUpperCase() + endDate.substring(1);

            // Use reflection to get startDate and endDate
            Method getStartDate = obj.getClass().getMethod(startMethod);
            Method getEndDate = obj.getClass().getMethod(endMethod);

            Date startDateValue = (Date) getStartDate.invoke(obj);
            Date endDateValue = (Date) getEndDate.invoke(obj);

            // If either date is null, allow it (use @NotNull for required validation)
            if (startDateValue == null || endDateValue == null) {
                return true;
            }

            // Check that endDate is after startDate
            return endDateValue.after(startDateValue);
        } catch (NoSuchMethodException e) {
            // If the methods don't exist, the validation passes
            // (this validator only applies to classes with these methods)
            return true;
        } catch (Exception e) {
            // For any other exception, fail validation to be safe
            return false;
        }
    }
}

