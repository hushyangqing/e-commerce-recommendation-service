package qyang.com.recommendation_service.test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class encodingPassword {
	public static void main(String[] args) {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		System.out.println("Encoded password1: " + encoder.encode("password1"));
		System.out.println("Encoded password2: " + encoder.encode("password2"));
		System.out.println("Encoded password: " + encoder.encode("password"));
	}
}
