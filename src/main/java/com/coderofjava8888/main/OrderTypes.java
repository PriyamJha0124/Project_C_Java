/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */
package com.coderofjava8888.main;

import com.ib.client.Order;

//  All orders must be market.


public class OrderTypes {

    public static Order Market(String action, double quantity) {
        //! [market]
        Order order = new Order();
        order.action(action);
        order.orderType("MKT");
        order.tif("GTC");
        order.totalQuantity(quantity);
        //! [market]
        return order;
    }

    
    
    

    public static Order MarketOrder(String action, double quantity) {
        //! [market]
        Order order = new Order();
        order.action(action);
        order.orderType("MKT");
        order.totalQuantity(quantity);
        //! [market]
        return order;
    }

    public static Order TrailingStop(String action, double quantity) {
        //! [market]
        Order order = new Order();
        order.action(action);
        order.orderType("TRAIL");
        order.totalQuantity(quantity);
        order.trailingPercent(0.02);
        order.trailStopPrice(100.00);
        //! [market]
        return order;
    }

//https://interactivebrokers.github.io/tws-api/basic_orders.html
    
    
}
