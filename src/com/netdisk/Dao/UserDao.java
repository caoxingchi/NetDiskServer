package com.netdisk.Dao;

import com.netdisk.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * @author xingchi
 * @create 2020/1/4
 */
public class UserDao {
    public int UpdateCapacity(User user, Connection con) throws Exception{
        String sql="update t_user set Capacity=?,usedCapacity=? where userName=?";
        PreparedStatement ps=con.prepareStatement(sql);
        ps.setDouble(1, user.getCapacity());
        ps.setDouble(2, user.getUsedCapacity());
        ps.setString(3, user.getUserName());
        return ps.executeUpdate();
    }
}
