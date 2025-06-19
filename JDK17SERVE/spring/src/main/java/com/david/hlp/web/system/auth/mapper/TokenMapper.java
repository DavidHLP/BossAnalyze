package com.david.hlp.web.system.auth.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.david.hlp.web.system.auth.token.Token;

@Mapper
public interface TokenMapper extends BaseMapper<Token> {

    @Select("SELECT id, user_id, token, token_type, expired, revoked, created_at FROM token WHERE token = #{token}")
    Token getByToken(String token);

    @Select("SELECT expired = FALSE AND revoked = FALSE FROM token WHERE token = #{token}")
    Boolean checkTokenValid(String token);

    @Insert("INSERT INTO token(user_id, token, token_type, expired, revoked) " +
            "VALUES(#{userId}, #{token}, #{tokenType}, #{expired}, #{revoked})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(Token token);

    @Select("SELECT id, user_id, token, token_type, expired, revoked, created_at FROM token WHERE user_id = #{userId} AND expired = false AND revoked = false")
    List<Token> listValidTokensByUser(Long userId);

    @Update({"<script>",
        "UPDATE token SET expired = #{expired}, revoked = #{revoked}",
        "WHERE token IN",
        "<foreach item='item' collection='list' open='(' separator=',' close=')'>",
        "#{item.token}",
        "</foreach>",
        "</script>"})
    void updateBatch(@Param("list") List<Token> tokens);
}