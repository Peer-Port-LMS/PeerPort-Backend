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
        this.startDate = constraintAnnotation.startDateField();
        this.endDate = constraintAnnotation.endDateField();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        // Allow null values (use @NotNull separately if required)
        if (obj == null) {
            return true;
        }

        try {
            // Build getter method names
            String startMethod = "get" + startDateField.substring(0,1).toUpperCase() + startDateField.substring(1);
            String endMethod = "get" + endDateField.substring(0,1).toUpperCase() + endDateField.substring(1);

            // Use reflection to get startDate and endDate
        Method getStartDate = obj.getClass().getMethod(startMethod);
            Method getEndDate = obj.getClass().getMethod(endMethod);

            Date startDate = (Date) getStartDate.invoke(obj);
            Date endDate = (Date) getEndDate.invoke(obj);

            // If either date is null, allow it (use @NotNull for required validation)
            if (startDate == null || endDate == null) {
                return true;
            }

            // Check that endDate is after startDate
            return endDate.after(startDate);
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

