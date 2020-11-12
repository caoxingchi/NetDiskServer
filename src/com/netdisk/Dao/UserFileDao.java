package com.netdisk.Dao;

import com.netdisk.model.UserFile;
import com.netdisk.utils.StringUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author xingchi
 * @create 2020/1/3
 */
public class UserFileDao {
    public int AddFile(Connection con, UserFile userFile) throws Exception{
        String sql="insert into userfiles"
                + " (fileid,userNum,fileName,"
                + "fileType,fileSize,updateTime,"
                + "fileFolderName,fileLocalPath,filePath)"
                + "values(null,?,?,?,?,?,?,?,?)";

        PreparedStatement ps = con.prepareStatement(sql);

        ps.setString(1, userFile.getUserNum());
        ps.setString(2, userFile.getFileName());
        ps.setString(3, userFile.getFileType());
        ps.setDouble(4, userFile.getFileSize());
        ps.setString(5, userFile.getUpdateTime());
        ps.setString(6, userFile.getFileFolder());
        ps.setString(7, userFile.getFileLocalPath());
        ps.setString(8, userFile.getFilePath());

        return ps.executeUpdate();

    }

    public ResultSet FileList(Connection con, UserFile userFile) throws Exception{
        ResultSet rs=null;
        StringBuffer sql=new StringBuffer("select * from userFiles uf,t_user u where "
                + "uf.userNum=u.userNum and u.userNum=?");
        //根据类型查询
        if(StringUtil.isNotEmpty(userFile.getFileType())) {
            sql.append(" and uf.fileType like'%" + userFile.getFileType()+ "%'");
        }
        //模糊查询
        if(StringUtil.isNotEmpty(userFile.getFileName())) {
            sql.append(" and uf.fileType like '%" + userFile.getFileName()+ "%'");
        }

        PreparedStatement ps = con.prepareStatement(sql.toString());
        ps.setString(1, userFile.getUserNum());
        return ps.executeQuery();
    }

    public int FileToRecycleBin(Connection con,String fileName,String userNum)throws Exception{
        String sql="update userfiles set fileType=? where fileName=? and userNum=?";
        PreparedStatement ps=con.prepareStatement(sql);
        ps.setString(1,"recycleBin");
        ps.setString(2,fileName);
        ps.setString(3,userNum);
        return ps.executeUpdate();
    }

}

