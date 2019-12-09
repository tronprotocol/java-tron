package org.tron.core.services.http.solidity.mockito;

import javax.servlet.http.HttpServletRequest;

/**
 * @author alberto
 * @version 1.0.0
 * @Description
 * @date 2019-12-08 22:51
 **/
public class AccountLoginController {

  private final AccountDao accountDao;

  public AccountLoginController(AccountDao accountDao) {
    this.accountDao = accountDao;
  }

  public String login(HttpServletRequest request) {
    final String userName = request.getParameter("username");
    final String password = request.getParameter("password");
    try{
      Account account = accountDao.findAccount(userName, password);
      if (account == null) {
        return "/login";
      }else {
        return "/index";
      }
    } catch (Exception e) {
      return "/505";
    }

  }
}
