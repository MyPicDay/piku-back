package store.piku.back.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import store.piku.back.global.entity.BaseEntity;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@Column(unique = true, nullable = false)
	private String email;

	private String password;

	@Column(unique = true, nullable = false)
	private String nickname;

	private String avatar;

	public User(String email, String password, String nickname, String avatar) {
		this.email = email;
		this.password = password;
		this.nickname = nickname;
		this.avatar = avatar;
	}

	public User(String email, String password, String nickname) {
		this.email = email;
		this.password = password;
		this.nickname = nickname;
	}

	public User(String id, String email, String password, String newNickname, String avatar) {
		this.id = id;
		this.email = email;
		this.password = password;
		this.nickname = newNickname;
		this.avatar = avatar;
	}

	/**
	 * 닉네임을 변경합니다.
	 * 
	 * @param newNickname 변경할 닉네임
	 */
	public void changeNickname(String newNickname) {
		this.nickname = newNickname;
	}

	/**
	 * 아바타를 변경합니다.
	 * 
	 * @param avatar 변경할 아바타 경로
	 */
	public void changeAvatar(String avatar) {
		this.avatar = avatar;
	}

	/**
	 * 비밀번호를 변경하는 비즈니스 메서드
	 * 
	 * @param newHashedPassword 암호화된 새로운 비밀번호
	 */
	public void updatePassword(String newHashedPassword) {
		this.password = newHashedPassword;
	}
}
