package com.pikume.back.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.pikume.back.global.entity.BaseEntity;
import com.pikume.back.user.domain.vo.Avatar;
import com.pikume.back.user.domain.vo.Email;
import com.pikume.back.user.domain.vo.Nickname;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
		@UniqueConstraint(name = "UK6dotkott2kjsp8vw4d0m25fb7", columnNames = "email"),
		@UniqueConstraint(name = "UK2ty1xmrrgtn89xt7kyxx6ta7h", columnNames = "nickname")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@Column(nullable = false)
	@Getter(AccessLevel.NONE)
	private Email email;

	private String password;

	@Column(nullable = false)
	@Getter(AccessLevel.NONE)
	private Nickname nickname;

	@Getter(AccessLevel.NONE)
	private Avatar avatar;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public User(String email, String password, String nickname, String avatar) {
		this.email = new Email(email);
		this.password = password;
		this.nickname = new Nickname(nickname);
		this.avatar = new Avatar(avatar);
	}

	public User(String email, String password, String nickname) {
		this.email = new Email(email);
		this.password = password;
		this.nickname = new Nickname(nickname);
		this.avatar = new Avatar(null);
	}

	public User(String id, String email, String password, String newNickname, String avatar) {
		this.id = id;
		this.email = new Email(email);
		this.password = password;
		this.nickname = new Nickname(newNickname);
		this.avatar = new Avatar(avatar);
	}

	/**
	 * 닉네임을 변경합니다.
	 * 
	 * @param newNickname 변경할 닉네임
	 */
	public void changeNickname(String newNickname) {
		this.nickname = new Nickname(newNickname);
	}

	/**
	 * 아바타를 변경합니다.
	 * 
	 * @param avatar 변경할 아바타 경로
	 */
	public void changeAvatar(String avatar) {
		this.avatar = new Avatar(avatar);
	}

	public String getEmail() {
		return email == null ? null : email.value();
	}

	public String getNickname() {
		return nickname == null ? null : nickname.value();
	}

	public String getAvatar() {
		return avatar == null ? null : avatar.path();
	}

	/**
	 * 비밀번호를 변경하는 비즈니스 메서드
	 * 
	 * @param newHashedPassword 암호화된 새로운 비밀번호
	 */
	public void updatePassword(String newHashedPassword) {
		this.password = newHashedPassword;
	}

	public void withdraw() {
		this.deletedAt = LocalDateTime.now();
	}

	public boolean isWithdrawn() {
		return deletedAt != null;
	}
}
