package com.david.hlp.contracts;

import com.david.hlp.web.system.auth.utils.JwtService;
import com.david.hlp.web.system.auth.mapper.TokenMapper;
import com.david.hlp.web.common.service.EmailService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.User;
import java.nio.file.Path;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SensitiveServiceContractTest {
    // Synthetic fixture, never a provider credential or deployment signing key.
    static final String KEY=Base64.getEncoder().encodeToString(new byte[32]);
    static final String MAIL="synthetic@example.invalid";
    static final String SECRET="synthetic-mail-only";
    static final String JWT="BOSSANALYZE_JWT_SECRET";
    @Configuration(proxyBeanMethods=false)
    @Import(JwtService.class)
    static class JwtConfig {
        @Bean TokenMapper tokens(){return mock(TokenMapper.class);}
        @Bean static PropertySourcesPlaceholderConfigurer placeholders(){return new PropertySourcesPlaceholderConfigurer();}
    }
    @Configuration(proxyBeanMethods=false)
    @Import(EmailService.class)
    static class MailConfig {
        @Bean JavaMailSender sender(){return mock(JavaMailSender.class);}
        @Bean static PropertySourcesPlaceholderConfigurer placeholders(){return new PropertySourcesPlaceholderConfigurer();}
    }
    Map<String,Object> yaml(boolean local) {
        var bean=new YamlPropertiesFactoryBean();
        var root=Path.of(System.getProperty("boss.source.root"),"JDK17SERVE/spring/src/main/resources");
        bean.setResources(new FileSystemResource(root.resolve("application.yml")),
            new FileSystemResource(root.resolve(local?"application-local-dev.yml":"application.yml")));
        Map<String,Object> result=new HashMap<>();bean.getObject().forEach((k,v)->result.put(k.toString(),v));return result;
    }
    ApplicationContextRunner runner(boolean local,Map<String,Object> override,Class<?> config) {
        return new ApplicationContextRunner().withInitializer(c->{
            var sources=c.getEnvironment().getPropertySources();
            sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
            sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
            sources.addFirst(new MapPropertySource("tracked-yaml",yaml(local)));
            sources.addFirst(new MapPropertySource("synthetic-only",override));
        }).withUserConfiguration(config);
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void jwtRejectsMissingAndInvalidAtInitialization(boolean local) {
        List<Map<String,Object>> cases=new ArrayList<>();cases.add(Map.of());
        for(String value:new String[]{""," ","your_example","${UNSET}","synthetic*invalid",Base64.getEncoder().encodeToString(new byte[31])})cases.add(Map.of(JWT,value));
        for(var props:cases)runner(local,props,JwtConfig.class).run(c->assertThat(c).hasFailed());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void stableKeyRetainsOldTokensAndNewAccessRefresh(boolean local) {
        var signingKey=Keys.hmacShaKeyFor(Decoders.BASE64.decode(KEY));
        String old=Jwts.builder().setSubject(MAIL).setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis()+60000)).signWith(signingKey,SignatureAlgorithm.HS256).compact();
        runner(local,Map.of(JWT,KEY),JwtConfig.class).run(c->{
            assertThat(c).hasNotFailed();var service=c.getBean(JwtService.class);
            assertThat(service.extractUsername(old)).isEqualTo(MAIL);
            var user=User.withUsername(MAIL).password("synthetic-user-only").authorities("ROLE_TEST").build();
            for(String token:List.of(service.generateToken(user),service.generateToken(Map.of("test","synthetic"),user),service.generateRefreshToken(user))){
                var claims=Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();
                assertThat(claims.getSubject()).isEqualTo(MAIL);assertThat(service.isTokenValid(token,user)).isTrue();
            }
            var refresh=Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(service.generateRefreshToken(user)).getBody();
            assertThat(refresh.getExpiration().getTime()-refresh.getIssuedAt().getTime()).isEqualTo(604800000L);
            verifyNoInteractions(c.getBean(TokenMapper.class));
        });
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void smtpMissingAllowsStartupButBothSendsFailBeforeInteractions(boolean local) {
        for(String bad:new String[]{""," ","${UNSET}","your_example"}) {
            for(boolean password:new boolean[]{false,true}) {
                Map<String,Object> props=Map.of("spring.mail.username",password?MAIL:bad,"spring.mail.password",password?bad:SECRET);
                // An unresolved Spring placeholder may fail injection before service creation.
                if(bad.startsWith("${")) {
                    runner(local,props,MailConfig.class).run(c->assertThat(c).hasFailed());
                    continue;
                }
                runner(local,props,MailConfig.class).run(c->{
                    assertThat(c).hasNotFailed();var service=c.getBean(EmailService.class);
                    assertThatThrownBy(()->service.sendSimpleMail(MAIL,"synthetic","synthetic")).isInstanceOf(IllegalStateException.class);
                    assertThatThrownBy(()->service.sendHtmlMail(MAIL,"synthetic","<b>synthetic</b>")).isInstanceOf(IllegalStateException.class);
                    verifyNoInteractions(c.getBean(JavaMailSender.class));
                });
            }
        }
        runner(local,Map.of(),MailConfig.class).run(c->{assertThat(c).hasNotFailed();var s=c.getBean(EmailService.class);assertThatThrownBy(()->s.sendSimpleMail(MAIL,"synthetic","synthetic")).isInstanceOf(IllegalStateException.class);verifyNoInteractions(c.getBean(JavaMailSender.class));});
    }
    @Test void explicitJwtPropertyOverridesEnvironmentWithoutRotation() {
        runner(false,Map.of(JWT,"synthetic-invalid*","application.security.jwt.secret-key",KEY),JwtConfig.class)
            .run(c->{assertThat(c).hasNotFailed();assertThat(c.getBean(JwtService.class)).isNotNull();});
    }
    @Test void validExternalKeySurvivesContextRestart() {
        var user=User.withUsername(MAIL).password("synthetic-user-only").authorities("ROLE_TEST").build();
        String[] issued=new String[2];
        runner(false,Map.of(JWT,KEY),JwtConfig.class).run(c->{
            issued[0]=c.getBean(JwtService.class).generateToken(user);
            issued[1]=c.getBean(JwtService.class).generateRefreshToken(user);
        });
        runner(false,Map.of(JWT,KEY),JwtConfig.class).run(c->{
            assertThat(c).hasNotFailed();
            for(String token:issued)assertThat(c.getBean(JwtService.class).isTokenValid(token,user)).isTrue();
        });
    }
    @Test void smtpBothPathsAndTransportFailuresRemainCompatible() {
        runner(false,Map.of("BOSSANALYZE_SMTP_USERNAME",MAIL,"BOSSANALYZE_SMTP_PASSWORD",SECRET),MailConfig.class).run(c->{
            assertThat(c).hasNotFailed();var service=c.getBean(EmailService.class);var sender=c.getBean(JavaMailSender.class);
            service.sendSimpleMail(MAIL,"synthetic-subject","synthetic-body");
            var capture=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);verify(sender).send(capture.capture());assertThat(capture.getValue().getFrom()).isEqualTo(MAIL);
            var mime=new MimeMessage(Session.getInstance(new Properties()));when(sender.createMimeMessage()).thenReturn(mime);
            service.sendHtmlMail(MAIL,"synthetic-subject","<b>synthetic-body</b>");verify(sender).send(mime);
            try {assertThat(mime.getFrom()[0].toString()).isEqualTo(MAIL);}catch(Exception e){throw new AssertionError("Synthetic MIME assertion failed");}
            var failure=new MailSendException("synthetic-transport-failure");doThrow(failure).when(sender).send(any(SimpleMailMessage.class));
            assertThatThrownBy(()->service.sendSimpleMail(MAIL,"synthetic","synthetic")).isSameAs(failure);
            doThrow(failure).when(sender).send(mime);
            assertThatThrownBy(()->service.sendHtmlMail(MAIL,"synthetic","synthetic")).isInstanceOf(RuntimeException.class).hasCause(failure);
        });
    }
}
