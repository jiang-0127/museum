package com.museum.reservation.module.statistics.mapper;

import com.museum.reservation.module.statistics.vo.NameValue;
import com.museum.reservation.module.statistics.vo.SlotSaturationVO;
import com.museum.reservation.module.statistics.vo.TrendPoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface StatisticsMapper {

    @Select("SELECT COUNT(*) FROM reservation WHERE DATE(created_at) = CURDATE()")
    long countTodayReservations();

    @Select("SELECT COALESCE(SUM(visitor_count), 0) FROM reservation WHERE DATE(created_at) = CURDATE() AND status <> 'CANCELED'")
    long countTodayVisitors();

    @Select("SELECT COUNT(*) FROM checkin_record WHERE DATE(checkin_time) = CURDATE()")
    long countTodayCheckins();

    @Select("SELECT COUNT(*) FROM reservation WHERE cancel_time IS NOT NULL AND DATE(cancel_time) = CURDATE()")
    long countTodayCanceled();

    @Select("SELECT COUNT(*) FROM payment_order WHERE status = 'UNPAID'")
    long countPendingPayments();

    @Select("SELECT COUNT(*) FROM museum WHERE status = 1")
    long countMuseums();

    @Select("SELECT COUNT(*) FROM sys_user WHERE status = 1")
    long countUsers();

    @Select("SELECT COUNT(*) FROM reservation")
    long countReservations();

    @Select("""
            SELECT DATE(created_at) AS date, COUNT(*) AS reservations,
                   COALESCE(SUM(visitor_count), 0) AS visitors
            FROM reservation
            WHERE created_at >= #{start} AND status <> 'CANCELED'
            GROUP BY DATE(created_at)
            ORDER BY date
            """)
    List<TrendPoint> selectReservationTrend(@Param("start") LocalDateTime start);

    @Select("""
            SELECT DATE(checkin_time) AS date, COUNT(*) AS checkins
            FROM checkin_record
            WHERE checkin_time >= #{start}
            GROUP BY DATE(checkin_time)
            ORDER BY date
            """)
    List<TrendPoint> selectCheckinTrend(@Param("start") LocalDateTime start);

    @Select("""
            SELECT DATE(cancel_time) AS date, COUNT(*) AS canceled
            FROM reservation
            WHERE cancel_time IS NOT NULL AND cancel_time >= #{start}
            GROUP BY DATE(cancel_time)
            ORDER BY date
            """)
    List<TrendPoint> selectCancelTrend(@Param("start") LocalDateTime start);

    @Select("""
            SELECT m.name AS name, COUNT(r.id) AS value
            FROM museum m
            LEFT JOIN reservation r ON r.museum_id = m.id
                AND r.status <> 'CANCELED' AND r.created_at >= #{start}
            GROUP BY m.id, m.name
            ORDER BY value DESC
            LIMIT 8
            """)
    List<NameValue> selectMuseumHeat(@Param("start") LocalDateTime start);

    @Select("""
            SELECT CASE t.ticket_kind
                       WHEN 'SINGLE' THEN '单展票'
                       WHEN 'COMBO' THEN '全馆联票'
                       ELSE '普通预约票'
                   END AS name,
                   COUNT(r.id) AS value
            FROM ticket_type t
            LEFT JOIN reservation r ON r.ticket_type_id = t.id AND r.status <> 'CANCELED'
            GROUP BY CASE t.ticket_kind
                         WHEN 'SINGLE' THEN '单展票'
                         WHEN 'COMBO' THEN '全馆联票'
                         ELSE '普通预约票'
                     END
            ORDER BY value DESC
            """)
    List<NameValue> selectTicketDistribution();

    @Select("""
            SELECT s.id, m.name AS museum_name, s.visit_date, s.start_time, s.end_time,
                   s.total_capacity, s.booked_count,
                   ROUND(s.booked_count * 100 / s.total_capacity) AS saturation
            FROM visit_slot s
            JOIN museum m ON m.id = s.museum_id
            WHERE s.visit_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY)
              AND s.status = 1
            ORDER BY saturation DESC, s.visit_date ASC
            LIMIT 10
            """)
    List<SlotSaturationVO> selectSlotSaturation();
}
