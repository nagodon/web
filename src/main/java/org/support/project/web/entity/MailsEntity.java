package org.support.project.web.entity;

import java.util.List;
import java.util.Map;
import java.sql.Timestamp;

import org.support.project.common.bean.ValidateError;
import org.support.project.di.Container;
import org.support.project.di.DI;
import org.support.project.di.Instance;
import org.support.project.web.entity.gen.GenMailsEntity;


/**
 * メール
 */
@DI(instance=Instance.Prototype)
public class MailsEntity extends GenMailsEntity {

	/** SerialVersion */
	private static final long serialVersionUID = 1L;

	/**
	 * インスタンス取得
	 * AOPに対応
	 * @return インスタンス
	 */
	public static MailsEntity get() {
		return Container.getComp(MailsEntity.class);
	}

	/**
	 * コンストラクタ
	 */
	public MailsEntity() {
		super();
	}

	/**
	 * コンストラクタ
	 * @param mailId MAIL_ID
	 */

	public MailsEntity(String mailId) {
		super( mailId);
	}

}
