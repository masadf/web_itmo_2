package servlets;

import exceptions.InvalidParameterException;
import models.Hit;
import models.Point;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;

import static java.lang.Math.pow;

public class AreaCheckServlet extends HttpServlet {
    public static final String HITS_DATA_ATTRIBUTE = "hitsData";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Date startExecution = new Date();
        try {
            Point point = buildPoint(req);
            validatePoint(point);
            Hit currentHit = Hit.builder()
                    .xVal(point.getXVal())
                    .yVal(point.getYVal())
                    .rVal(point.getRVal())
                    .currentTime(ZonedDateTime.now().minusMinutes(point.getTimezone()).toLocalDateTime())
                    .executionTime(new Date().getTime() - startExecution.getTime())
                    .isHit(isHit(point))
                    .build();

            HttpSession session = req.getSession();
            saveHitInSession(session, currentHit);
            fillResponse(resp, session);
        } catch (InvalidParameterException e) {
            resp.sendError(403, e.getMessage());
        }
    }

    private Point buildPoint(HttpServletRequest req) {
        double xVal;
        double yVal;
        double rVal;
        long timezone;

        try {
            xVal = Double.parseDouble(req.getParameter("xVal"));
            yVal = Double.parseDouble(req.getParameter("yVal"));
            rVal = Double.parseDouble(req.getParameter("rVal"));
            timezone = Long.parseLong(req.getParameter("timezone"));
        } catch (NumberFormatException exception) {
            throw new InvalidParameterException("Некорректный формат параметров!");
        }

        return new Point(xVal, yVal, rVal, timezone);
    }

    private void validatePoint(Point point) {
        byte MAX_Y = 5;
        byte MIN_Y = -5;

        if (!(point.getYVal() > MIN_Y && point.getYVal() < MAX_Y)) {
            throw new InvalidParameterException("Значение Y не попадает в нужный интервал!");
        }

        if (point.getRVal() <= 0) {
            throw new InvalidParameterException("Значение R не может быть неположительным!");
        }
    }

    private boolean isHit(Point point) {
        return isRectangleHit(point) || isCircleHit(point) || isTriangleHit(point);
    }

    private boolean isRectangleHit(Point point) {
        return point.getXVal() <= 0 && point.getXVal() >= -point.getRVal()
                && point.getYVal() >= 0 && point.getYVal() <= point.getRVal() / 2;
    }

    private boolean isCircleHit(Point point) {
        return point.getXVal() >= 0 && point.getYVal() >= 0
                && (pow(point.getXVal(), 2) + pow(point.getYVal(), 2)) <= pow(point.getRVal() / 2, 2);
    }

    private boolean isTriangleHit(Point point) {
        return point.getXVal() >= 0 && point.getYVal() <= 0
                && (point.getXVal() - point.getRVal()) <= point.getYVal();
    }

    private void saveHitInSession(HttpSession session, Hit hit) {
        ArrayList<Hit> allHits = getHits(session);

        try {
            allHits.add(hit);
        } catch (NullPointerException e) {
            allHits = new ArrayList<Hit>();
            allHits.add(hit);
        }

        session.setAttribute(HITS_DATA_ATTRIBUTE, allHits);
    }

    private void fillResponse(HttpServletResponse resp, HttpSession session) throws IOException {
        resp.setContentType("text/html;charset=UTF-8");

        PrintWriter writer = resp.getWriter();
        writer.print(getTableHtml(session));
    }

    private String getTableHtml(HttpSession session) {
        StringBuilder tableBuilder = new StringBuilder();

        tableBuilder.append("<table>")
                .append("<tbody>")
                .append("<tr>")
                .append("<th>X</th>")
                .append("<th>Y</th>")
                .append("<th>R</th>")
                .append("<th>Время попытки</th>")
                .append("<th>Длительность</th>")
                .append("<th>Попадание</th>")
                .append("</tr>");

        getHits(session).forEach((hit) -> {
            tableBuilder.append("<tr>")
                    .append("<td>").append(hit.getXVal()).append("</td>")
                    .append("<td>").append(hit.getYVal()).append("</td>")
                    .append("<td>").append(hit.getRVal()).append("</td>")
                    .append("<td>").append(hit.getCurrentTime()).append("</td>")
                    .append("<td>").append(hit.getExecutionTime()).append("</td>")
                    .append("<td>").append(hit.isHit()).append("</td>")
                    .append("</tr>");
        });

        tableBuilder.append("</tbody>");
        tableBuilder.append("</table>");

        return tableBuilder.toString();
    }

    private ArrayList<Hit> getHits(HttpSession session) {
        return (ArrayList<Hit>) session.getAttribute(HITS_DATA_ATTRIBUTE);
    }
}
