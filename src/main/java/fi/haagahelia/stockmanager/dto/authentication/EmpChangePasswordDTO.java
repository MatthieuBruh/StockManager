package fi.haagahelia.stockmanager.dto.authentication;


import lombok.Data;

@Data
public class EmpChangePasswordDTO {
    private String currentPassword;
    private String newPassword;
    private String newPasswordVerification;

    public boolean isValid() {
        if (currentPassword == null) return false;
        if (newPassword == null || newPassword.length() < 8) return false;
        if (newPasswordVerification == null || newPasswordVerification.length() < 8) return false;
        if (!newPassword.equals(newPasswordVerification)) return false;
        return true;
    }
}
