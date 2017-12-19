package org.tron.peer;

public class ValidationRuleFactory {
    public static ValidationRule create(String type) {
        if (type.equals("Validation")) {
            return new Validation();
        }

        return null;
    }
}
