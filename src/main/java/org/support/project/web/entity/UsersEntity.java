package org.support.project.web.entity;

import java.util.Locale;

import org.support.project.di.Container;
import org.support.project.di.DI;
import org.support.project.di.Instance;
import org.support.project.web.dao.LocalesDao;
import org.support.project.web.entity.gen.GenUsersEntity;


/**
 * ユーザ
 */
@DI(instance=Instance.Prototype)
public class UsersEntity extends GenUsersEntity {

	/** SerialVersion */
	private static final long serialVersionUID = 1L;
	/** 既に暗号化済かどうか */
	private Boolean encrypted = Boolean.FALSE;
	/** 管理者かどうか */
	private Boolean admin = Boolean.FALSE;
	
	
	
	@Override
	protected String convLabelName(String label) {
		if ("User Key".equals(label)) {
			return "Mail Address";
		}
		return super.convLabelName(label);
	}

	/**
	 * インスタンス取得
	 * AOPに対応
	 * @return インスタンス
	 */
	public static UsersEntity get() {
		return Container.getComp(UsersEntity.class);
	}

	/**
	 * コンストラクタ
	 */
	public UsersEntity() {
		super();
	}

	/**
	 * コンストラクタ
	 * @param userId ユーザID
	 */

	public UsersEntity(Integer userId) {
		super( userId);
	}
	
	/**
	 * ユーザのロケールを取得
	 * @param usersEntity
	 * @return
	 */
	public Locale getLocale() {
		Locale locale = Locale.JAPAN; // default
		LocalesDao localesDao = LocalesDao.get();
		LocalesEntity localesEntity = localesDao.selectOnKey(this.getLocaleKey());
		if (localesEntity != null) {
			locale = new Locale(localesEntity.getLanguage(), localesEntity.getCountry(), localesEntity.getVariant());
		}
		return locale;
	}
	
	
	public Boolean getEncrypted() {
		return encrypted;
	}

	public void setEncrypted(Boolean encrypted) {
		this.encrypted = encrypted;
	}

	public Boolean getAdmin() {
		return admin;
	}

	public void setAdmin(Boolean admin) {
		this.admin = admin;
	}

}
