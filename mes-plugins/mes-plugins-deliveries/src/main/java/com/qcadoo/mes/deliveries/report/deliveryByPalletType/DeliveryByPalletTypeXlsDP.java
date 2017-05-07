package com.qcadoo.mes.deliveries.report.deliveryByPalletType;

import com.google.common.collect.Maps;
import com.qcadoo.model.api.DataDefinitionService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
class DeliveryByPalletTypeXlsDP {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private DataDefinitionService dataDefinitionService;

    public Map<DeliveryByPalletTypeKey, DeliveryByPalletTypeValue> findEntries(final Map<String, Object> filters) {
        Map<String, Object> _filters = (Map<String, Object>) filters.get("params");
        Long from = (Long) _filters.get("from");
        Long to = (Long) _filters.get("to");
        Map<String, Object> params = Maps.newHashMap();
        params.put("fromDate", new Date(from));
        params.put("toDate", new DateTime(to).plusDays(1).toDate());
        String query = buildQuery();
        List<DeliveryByPalletTypeEntry> entries = jdbcTemplate.query(query, params, new BeanPropertyRowMapper(
                DeliveryByPalletTypeEntry.class));
        Map<DeliveryByPalletTypeKey, DeliveryByPalletTypeValue> deliveryByPallet = Maps.newLinkedHashMap();

        for (DeliveryByPalletTypeEntry entry : entries) {
            DeliveryByPalletTypeKey key = new DeliveryByPalletTypeKey(entry);
            if (deliveryByPallet.containsKey(key)) {
                DeliveryByPalletTypeValue value = deliveryByPallet.get(key);
                value.addQuantityForPallet(entry.getPalletType(), entry.getNumberOfPallets());
            } else {
                DeliveryByPalletTypeValue value = new DeliveryByPalletTypeValue();
                value.addQuantityForPallet(entry.getPalletType(), entry.getNumberOfPallets());
                deliveryByPallet.put(key, value);
            }
        }
        return deliveryByPallet;
    }

    private String buildQuery() {
        StringBuilder query = new StringBuilder();
        query.append("SELECT delivery.id, delivery.number, deliveredproduct.pallettype, deliverystatechange.dateandtime as date, ");
        query.append("count(DISTINCT deliveredproduct.palletnumber_id) as numberofpallets FROM deliveries_delivery delivery ");
        query.append("LEFT JOIN deliveries_deliveredproduct deliveredproduct ON deliveredproduct.delivery_id = delivery.id ");
        query.append("LEFT JOIN deliveries_deliverystatechange deliverystatechange ON deliverystatechange.delivery_id = delivery.id AND deliverystatechange.status = '03successful' AND deliverystatechange.targetstate = '06received' ");
        query.append("WHERE delivery.state = '06received' AND deliverystatechange.dateandtime >= :fromDate AND deliverystatechange.dateandtime <= :toDate ");
        query.append("GROUP BY delivery.id, delivery.number, deliverystatechange.dateandtime, deliveredproduct.pallettype ");
        query.append("ORDER BY deliverystatechange.dateandtime ASC, delivery.number");
        return query.toString();
    }
}