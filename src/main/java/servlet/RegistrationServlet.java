package servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import dto.CreateAccountDto;
import entity.Country;
import entity.Gender;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.PersonalAccountService;
import utlis.JspHelper;

@WebServlet("/registration")
public class RegistrationServlet extends HttpServlet {

	private final static String USER = "User";
	private final static String AUTHORIZATION_STATUS = "AuthorizationStatus";
	private final static String LOCAL_COOKIE = "LocalInfo";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
		resp.setContentType("text/html");

		if (req.getSession().getAttribute(USER) != null) {
			resp.sendRedirect("/items");
			return;
		}

		req.setAttribute("countries", Country.values());
		req.setAttribute("genders", Gender.values());
		req.getRequestDispatcher(JspHelper.getUrl("registration")).forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
		resp.setContentType("text/html");
		var cookies = req.getCookies();
		boolean cookieIsEmpty = Arrays.stream(cookies).filter(cookie -> cookie.getName().equals(LOCAL_COOKIE))
				.findFirst().isEmpty() || cookies == null;
		if (cookieIsEmpty) {
			Cookie localCookie = new Cookie(LOCAL_COOKIE, req.getLocale().toString());
			resp.addCookie(localCookie);
		}
		var session = req.getSession();
		var country = req.getParameter("country");
		var city = req.getParameter("city");
		var address = req.getParameter("address");
		var phoneNumber = req.getParameter("phoneNumber");
		CreateAccountDto.builder().email(req.getParameter("email")).password(req.getParameter("password"))
				.name(req.getParameter("name")).surname(req.getParameter("surname"))
				.birthday(req.getParameter("dateOfBirth")).country(req.getParameter("country"))
				.city(req.getParameter("city")).address(req.getParameter("address"))
				.phoneNumber(req.getParameter("phoneNumber")).gender(req.getParameter("gender")).build();
	}

}