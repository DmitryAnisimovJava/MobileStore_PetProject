package servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.ItemsService;

@WebServlet("/items")
public class ItemsServlet extends HttpServlet {

	private final ItemsService instanceService = ItemsService.getInstance();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html");
		resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

		try (PrintWriter printWriter = resp.getWriter()) {
			printWriter.write("<h1>Список телефонов</h1>");
			printWriter.write("<ul>");
			instanceService.itemsServiceMethod().forEach(item -> {
				printWriter.write("""
						<li>
							<a href="/items-parameters?itemId=%d"> %s %s %,.2f %s </a>
						</li>
						""".formatted(item.getItemId(), item.getBrand(), item.getModel(), item.getPrice(),
						item.getCurrency()));
			});
			printWriter.write("</ul>");
		}
	}
}