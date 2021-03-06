package org.support.project.web.logic.impl;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.support.project.common.config.ConfigLoader;
import org.support.project.common.util.PasswordUtil;
import org.support.project.di.DI;
import org.support.project.di.Instance;
import org.support.project.ormapping.common.DBUserPool;
import org.support.project.web.bean.LoginedUser;
import org.support.project.web.common.HttpUtil;
import org.support.project.web.config.AppConfig;
import org.support.project.web.config.CommonWebParameter;
import org.support.project.web.dao.FunctionsDao;
import org.support.project.web.dao.GroupsDao;
import org.support.project.web.dao.RoleFunctionsDao;
import org.support.project.web.dao.RolesDao;
import org.support.project.web.dao.UsersDao;
import org.support.project.web.entity.FunctionsEntity;
import org.support.project.web.entity.GroupsEntity;
import org.support.project.web.entity.RoleFunctionsEntity;
import org.support.project.web.entity.RolesEntity;
import org.support.project.web.entity.UsersEntity;
import org.support.project.web.exception.AuthenticateException;
import org.support.project.web.logic.AuthenticationLogic;


@DI(instance=Instance.Singleton)
public abstract class AbstractAuthenticationLogic<T extends LoginedUser> implements AuthenticationLogic<T> {
	/**
	 * ロールが必要な機能のリスト
	 */
	private Map<String, List<Integer>> roleRequireFunctionList = null;
	
	private boolean init = false;
	
	/**
	 * コンストラクタ
	 */
	public AbstractAuthenticationLogic() {
		super();
	}
	
	/**
	 * 初期化処理
	 */
	protected void initLogic() {
		if (!init) {
			//ロールと機能のリストを読み込む
			roleRequireFunctionList = new HashMap<String, List<Integer>>();
			FunctionsDao functionsDao = FunctionsDao.get();
			RoleFunctionsDao roleFunctionsDao = RoleFunctionsDao.get();
			
			List<FunctionsEntity> functionsEntities = functionsDao.selectAll();
			for (FunctionsEntity functionsEntity : functionsEntities) {
				List<RoleFunctionsEntity> roleFunctionsEntities = roleFunctionsDao.selectOnFunction(functionsEntity.getFunctionKey());
				List<Integer> roles = new ArrayList<>();
				for (RoleFunctionsEntity roleFunctionsEntity : roleFunctionsEntities) {
					roles.add(roleFunctionsEntity.getRoleId());
				}
				roleRequireFunctionList.put(functionsEntity.getFunctionKey(), roles);
			}
			init = true;
		}
	}
	
	/**
	 * 認証
	 * デフォルトでは、Database認証
	 */
	@Override
	public boolean auth(String userId, String password)
			throws AuthenticateException {
		if (!init) {
			initLogic();
		}
		
		try {
			UsersDao usersDao = UsersDao.get();
			UsersEntity usersEntity = usersDao.selectOnUserKey(userId);
			
			AppConfig config = ConfigLoader.load(AppConfig.APP_CONFIG, AppConfig.class);
			if (usersEntity != null) {
				String hash = PasswordUtil.getStretchedPassword(password, usersEntity.getSalt(), config.getHashIterations());
				if (usersEntity.getPassword().equals(hash)) {
					return true;
				}
			}
			return false;
		} catch (NoSuchAlgorithmException e) {
			throw new AuthenticateException(e);
		}
	}

	/**
	 * ログインしているかチェック
	 */
	@Override
	public boolean isLogined(HttpServletRequest request)
			throws AuthenticateException {
		if (getSession(request) != null) {
			setDBUser(request);
			return true;
		} else {
			DBUserPool.get().clearUser();
		}
		return false;
	}
	
	/**
	 * セッション情報を作成
	 */
	@Override
	public void setSession(String userId, HttpServletRequest request)
			throws AuthenticateException {
		try {
			HttpSession session = request.getSession();
			session.setAttribute(CommonWebParameter.LOGIN_USER_ID_SESSION_KEY, userId);
			
			UsersDao usersDao = UsersDao.get();
			UsersEntity usersEntity = usersDao.selectOnUserKey(userId);
			RolesDao rolesDao = RolesDao.get();
			List<RolesEntity> rolesEntities = rolesDao.selectOnUserKey(userId);
			session.setAttribute(CommonWebParameter.LOGIN_ROLE_IDS_SESSION_KEY, rolesEntities);
			
			LoginedUser loginedUser = new LoginedUser();
			loginedUser.setLoginUser(usersEntity);
			loginedUser.setRoles(rolesEntities);
			loginedUser.setLocale(HttpUtil.getLocale(request));
			
			// グループ
			GroupsDao groupsDao = GroupsDao.get();
			List<GroupsEntity> groups = groupsDao.selectMyGroup(loginedUser, 0, Integer.MAX_VALUE);
			loginedUser.setGroups(groups);
			
			session.setAttribute(CommonWebParameter.LOGIN_USER_INFO_SESSION_KEY, loginedUser);
			
			setDBUser(request);
		} catch (Exception e) {
			throw new AuthenticateException(e);
		}
		
	}
	
	/**
	 * 認可
	 * Databaseの認可情報により、指定のパスにアクセス出来るかチェック
	 * ※Controlのインタフェースメソッドに、アクセス出来るロールが指定されているが、
	 * そのチェックとは別なので、わかりづらいかも。。。
	 * Controlにある認可チェックは、ControlManagerFilterで処理している。
	 */
	@Override
	public boolean isAuthorize(HttpServletRequest request)
			throws AuthenticateException {
		if (!init) {
			initLogic();
		}

		String path = request.getServletPath();
		
		Iterator<String> iterator = roleRequireFunctionList.keySet().iterator();
		while (iterator.hasNext()) {
			String function = (String) iterator.next();
			if (path.startsWith(function)) {
				HttpSession session = request.getSession();
				LoginedUser loginedUser = 
						(LoginedUser) session.getAttribute(CommonWebParameter.LOGIN_USER_INFO_SESSION_KEY);
				
				if (loginedUser == null) {
					return false;
				}
				
				List<Integer> accessRoles = roleRequireFunctionList.get(function);
				List<RolesEntity> userRoles = loginedUser.getRoles();
				for (RolesEntity userRole : userRoles) {
					if (accessRoles.contains(userRole.getRoleId())) {
						return true;
					}
				}
				return false;
			}
		}
		//デフォルトはアクセス可能
		return true;
	}

	/**
	 * ログインセッション情報を取得
	 */
	@Override
	public T getSession(HttpServletRequest request)
			throws AuthenticateException {
		HttpSession session = request.getSession();
		return (T) session.getAttribute(CommonWebParameter.LOGIN_USER_INFO_SESSION_KEY);
	}
	
	/**
	 * ログインセッション情報をクリア
	 */
	@Override
	public void clearSession(HttpServletRequest request)
			throws AuthenticateException {
		HttpSession session = request.getSession();
		session.removeAttribute(CommonWebParameter.LOGIN_USER_ID_SESSION_KEY);
		session.removeAttribute(CommonWebParameter.LOGIN_ROLE_IDS_SESSION_KEY);
		session.removeAttribute(CommonWebParameter.LOGIN_USER_INFO_SESSION_KEY);
	}

	/**
	 * このリクエストを処理する間のユーザIDをセットしておく
	 * @param request
	 */
	protected void setDBUser(HttpServletRequest request) {
		LoginedUser loginedUser = getSession(request);
		DBUserPool.get().setUser(loginedUser.getLoginUser().getUserId());
	}
	
	
	
}
