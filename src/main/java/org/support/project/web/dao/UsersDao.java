package org.support.project.web.dao;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.support.project.common.config.ConfigLoader;
import org.support.project.common.exception.SystemException;
import org.support.project.common.util.PasswordUtil;
import org.support.project.common.util.StringUtils;
import org.support.project.di.Container;
import org.support.project.di.DI;
import org.support.project.di.Instance;
import org.support.project.ormapping.common.SQLManager;
import org.support.project.web.config.AppConfig;
import org.support.project.web.dao.gen.GenUsersDao;
import org.support.project.web.entity.UsersEntity;

/**
 * ユーザ
 */
@DI(instance=Instance.Singleton)
public class UsersDao extends GenUsersDao {

	/** SerialVersion */
	private static final long serialVersionUID = 1L;
	/**
	 * インスタンス取得
	 * AOPに対応
	 * @return インスタンス
	 */
	public static UsersDao get() {
		return Container.getComp(UsersDao.class);
	}


	/**
	 * ID 
	 */
	private int currentId = 0;

	/**
	 * IDを採番 
	 * ※コミットしなくても次のIDを採番する為、保存しなければ欠番になる 
	 */
	public Integer getNextId() {
		String sql = "SELECT MAX(USER_ID) FROM USERS;";
		Integer integer = executeQuerySingle(sql, Integer.class);
		if (integer != null) {
			if (currentId < integer) {
				currentId = integer;
			}
		}
		currentId++;
		return currentId;
	}

	@Override
	public UsersEntity physicalInsert(UsersEntity entity) {
		//DBに保存する直前に暗号化する
		passwordEncrypted(entity);
		return super.physicalInsert(entity);
	}

	@Override
	public UsersEntity physicalUpdate(UsersEntity entity) {
		//DBに保存する直前に暗号化する
		passwordEncrypted(entity);
		return super.physicalUpdate(entity);
	}
	
	/**
	 * DBに保存する直前に暗号化する
	 * @param entity
	 */
	private void passwordEncrypted(UsersEntity entity) {
		try {
			if (!entity.getEncrypted()) {
				String salt = PasswordUtil.getSalt();
				entity.setSalt(salt);
				AppConfig config = ConfigLoader.load(AppConfig.APP_CONFIG, AppConfig.class);
				entity.setPassword(PasswordUtil.getStretchedPassword(entity.getPassword(), salt, config.getHashIterations()));
			}
		} catch (NoSuchAlgorithmException e) {
			throw new SystemException(e);
		}
	}
	
	
	
	
	/**
	 * ユーザのキーでユーザ情報を取得
	 * @param userKey
	 * @return
	 */
	public UsersEntity selectOnUserKey(String userKey) {
		String sql = "SELECT * FROM USERS WHERE USER_KEY = ?;";
		return executeQuerySingle(sql, UsersEntity.class, userKey);
	}


	/**
	 * ロールIDを指定してユーザ情報を取得
	 * @param roleId
	 * @return
	 */
	public List<UsersEntity> selectOnRoleId(Integer roleId) {
		String sql = SQLManager.getInstance().getSql("/org/support/project/web/dao/sql/UsersDao/UsersDao_selectOnRoleId.sql");
		return executeQueryList(sql, UsersEntity.class, roleId);
	}
	
	/**
	 * ロール名を指定してユーザ情報を取得
	 * @param roleKey
	 * @return
	 */
	public List<UsersEntity> selectOnRoleKey(String roleKey) {
		String sql = SQLManager.getInstance().getSql("/org/support/project/web/dao/sql/UsersDao/UsersDao_selectOnRoleKey.sql");
		return executeQueryList(sql, UsersEntity.class, roleKey);
	}
	
	
	/* (非 Javadoc)
	 * @see org.support.project.transparent.base.dao.gen.GenUsersDao#selectOnKey(java.lang.Integer)
	 */
	@Override
	public UsersEntity selectOnKey(Integer userId) {
		UsersEntity entity = super.selectOnKey(userId);
		if (entity != null) {
			entity.setEncrypted(Boolean.TRUE);
		}
		return entity;
	}

	public List<UsersEntity> selectOnKeyword(int offset, int pageLimit, String keyword) {
		StringBuilder sql = new StringBuilder();
		List<Object> params = new ArrayList<Object>();
		sql.append("SELECT * FROM USERS WHERE DELETE_FLAG = 0 ");
		if (!StringUtils.isEmpty(keyword)) {
			sql.append("AND USER_NAME LIKE ? ");
			params.add("%" + keyword + "%");
		}
		sql.append("ORDER BY USER_ID ASC Limit ? offset ?;");
		params.add(pageLimit);
		params.add(offset);
		return executeQueryList(sql.toString(), UsersEntity.class, params.toArray());
	}

	/**
	 * IDを複数指定してユーザの一覧を取得
	 * @param userids
	 * @return
	 */
	public List<UsersEntity> selectOnUserIds(List<Integer> userids) {
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT * FROM USERS WHERE USER_ID IN (");
		int cnt = 0;
		for (Integer integer : userids) {
			if (cnt > 0) {
				builder.append(",");
			}
			builder.append("?");
			cnt++;
		}
		builder.append(") ORDER BY USER_ID");
		return executeQueryList(builder.toString(), UsersEntity.class, userids.toArray(new Integer[0]));
	}
	
	

}
