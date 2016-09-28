/**
 * Created by anton.bezkrovny on 21.09.2016.
 */
public class Fishing {
    private String email;
    private int daysAfterChangePwd;
    private String name;
    private int uac;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getDaysAfterChangePwd() {
        return daysAfterChangePwd;
    }

    public void setDaysAfterChangePwd(int daysAfterChangePwd) {
        this.daysAfterChangePwd = daysAfterChangePwd;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUac() {
        return uac;
    }

    public void setUac(int uac) {
        this.uac = uac;
    }

    @Override
    public String toString() {
        return "Fishing{" +
                "email='" + email + '\'' +
                ", daysAfterChangePwd=" + daysAfterChangePwd +
                ", name='" + name + '\'' +
                ", uac=" + uac +
                '}';
    }
}
