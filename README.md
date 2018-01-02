Spring Boot : 使用 Zuul 实现 API Gateway 的路由和过滤 ( Routing and Filtering )

![image.png](http://upload-images.jianshu.io/upload_images/1233356-b9427984c61506f6.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


本节通过使用 Netflix Zuul 实现微服应用中的路由（简单代理转发）和过滤功能。

 > API Gateway 的搭建工作，技术选型是 Netflix Zuul

API Gateway 是随着微服务（Microservice）这个概念一起兴起的一种架构模式，它用于解决微服务过于分散，没有一个统一的出入口进行流量管理的问题。

Netflix Zuul 中提供了 Service Discovery (Eureka), Circuit Breaker (Hystrix), Intelligent Routing (Zuul) and Client Side Load Balancing (Ribbon) 等功能。

Spring Boot 是构建单个微服务应用的理想选择，但是我们还需要以某种方式将它们互相联系起来。这就是 Spring Cloud Netflix 所要解决的问题。Netflix 它提供了各种组件，比如：Eureka服务发现与Ribbon客户端负载均衡的结合，为内部“微服务”提供通信支持。但是，如果你想要与外界通信时（你提供外部API，或只是从你的页面使用AJAX），将各种服务隐藏在一个代理之后是一个明智的选择。

常规的选择我们会使用Nginx作为代理。但是Netflix带来了它自己的解决方案——智能路由Zuul。它带有许多有趣的功能，它可以用于身份验证、服务迁移、分级卸载以及各种动态路由选项。同时，它是使用Java编写的。

和大部分基于Java的Web应用类似，Zuul也采用了servlet架构，因此Zuul处理每个请求的方式是针对每个请求是用一个线程来处理。通常情况下，为了提高性能，所有请求会被放到处理队列中，从线程池中选取空闲线程来处理该请求。这样的设计方式，足以应付一般的高并发场景。

Zuul 是在云平台上提供动态路由，监控，弹性，安全等边缘服务的框架。Zuul 相当于是设备和 Netflix 流应用的 Web 网站后端所有请求的前门。Zuul 可以适当的对多个 Amazon Auto Scaling Groups 进行路由请求。

其架构如下图所示：

 ![image](http://upload-images.jianshu.io/upload_images/1233356-a1d5173cccadea6f.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

 Zuul提供了一个框架，可以对过滤器进行动态的加载，编译，运行。过滤器之间没有直接的相互通信。他们是通过一个RequestContext的静态类来进行数据传递的。RequestContext类中有ThreadLocal变量来记录每个Request所需要传递的数据。

过滤器是由Groovy写成。这些过滤器文件被放在Zuul Server上的特定目录下面。Zuul会定期轮询这些目录。修改过的过滤器会动态的加载到Zuul Server中以便于request使用。

下面有几种标准的过滤器类型：

*   PRE：这种过滤器在请求到达Origin Server之前调用。比如身份验证，在集群中选择请求的Origin Server，记log等。
*   ROUTING：在这种过滤器中把用户请求发送给Origin Server。发送给Origin Server的用户请求在这类过滤器中build。并使用Apache HttpClient或者Netfilx Ribbon发送给Origin Server。
*   POST：这种过滤器在用户请求从Origin Server返回以后执行。比如在返回的response上面加response header，做各种统计等。并在该过滤器中把response返回给客户。
*   ERROR：在其他阶段发生错误时执行该过滤器。
*   客户定制：比如我们可以定制一种STATIC类型的过滤器，用来模拟生成返回给客户的response。

过滤器的生命周期如下所示：

![image](http://upload-images.jianshu.io/upload_images/1233356-89166d91e67d62fd.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)



就像上图中所描述的一样，Zuul 提供了四种过滤器的 API，分别为前置（Pre）、后置（Post）、路由（Route）和错误（Error）四种处理方式。

一个请求会先按顺序通过所有的前置过滤器，之后在路由过滤器中转发给后端应用，得到响应后又会通过所有的后置过滤器，最后响应给客户端。在整个流程中如果发生了异常则会跳转到错误过滤器中。

一般来说，如果需要在请求到达后端应用前就进行处理的话，会选择前置过滤器，例如鉴权、请求转发、增加请求参数等行为。在请求完成后需要处理的操作放在后置过滤器中完成，例如统计返回值和调用时间、记录日志、增加跨域头等行为。路由过滤器一般只需要选择 Zuul 中内置的即可，错误过滤器一般只需要一个，这样可以在 Gateway 遇到错误逻辑时直接抛出异常中断流程，并直接统一处理返回结果。

  Zuul可以通过加载动态过滤机制，从而实现以下各项功能：

*   验证与安全保障: 识别面向各类资源的验证要求并拒绝那些与要求不符的请求。
*   审查与监控: 在边缘位置追踪有意义数据及统计结果，从而为我们带来准确的生产状态结论。
*   动态路由: 以动态方式根据需要将请求路由至不同后端集群处。
*   压力测试: 逐渐增加指向集群的负载流量，从而计算性能水平。
*   负载分配: 为每一种负载类型分配对应容量，并弃用超出限定值的请求。
*   静态响应处理: 在边缘位置直接建立部分响应，从而避免其流入内部集群。
*   多区域弹性: 跨越AWS区域进行请求路由，旨在实现ELB使用多样化并保证边缘位置与使用者尽可能接近。

除此之外，Netflix公司还利用Zuul的功能通过金丝雀版本实现精确路由与压力测试。








### Features

##### Service Discovery

Eureka instances can be registered and clients can discover the instances using Spring-managed beans

An embedded Eureka server can be created with declarative Java configuration

##### Circuit Breaker

Hystrix clients can be built with a simple annotation-driven method decorator

Embedded Hystrix dashboard with declarative Java configuration

##### Declarative REST Client

 Feign creates a dynamic implementation of an interface decorated with JAX-RS or Spring MVC annotations


##### Client Side Load Balancer

Ribbon

##### External Configuration

A bridge from the Spring Environment to Archaius (enables native configuration of Netflix components using Spring Boot conventions)


##### Router and Filter

automatic registration of Zuul filters, and a simple convention over configuration approach to reverse proxy creation



安装 Zuul 服务。它的应用本身很简单：
```java
@SpringBootApplication
@Controller
@EnableZuulProxy
public class DemoApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(DemoApplication.class)
            .web(true).run(args);
    }
}
```

我们还需要在 application.yml中定义固定的路由规则：

```
zuul:
  routes:
    sodik:
      path: /sodik/**
      url: http://target
```

现在我们试试运行测试：
```
$ curl http://proxy:8080/sodik/sample.html
```





# 应用场景

以下介绍一些 Zuul 中不同过滤器的应用场景。

## 前置过滤器

### 鉴权

一般来说整个服务的鉴权逻辑可以很复杂。

*   客户端：App、Web、Backend
*   权限组：用户、后台人员、其他开发者
*   实现：OAuth、JWT
*   使用方式：Token、Cookie、SSO

而对于后端应用来说，它们其实只需要知道请求属于谁，而不需要知道为什么，所以 Gateway 可以友善的帮助后端应用完成鉴权这个行为，并将用户的唯一标示透传到后端，而不需要、甚至不应该将身份信息也传递给后端，防止某些应用利用这些敏感信息做错误的事情。

Zuul 默认情况下在处理后会删除请求的 `Authorization` 头和 `Set-Cookie` 头，也算是贯彻了这个原则。

### 流量转发

流量转发的含义就是将指向 `/a/xxx.json` 的请求转发到指向 `/b/xxx.json` 的请求。这个功能可能在一些项目迁移、或是灰度发布上会有一些用处。

在 Zuul 中并没有一个很好的办法去修改 Request URI。在某些 [Issue](https://github.com/spring-cloud/spring-cloud-netflix/issues/435) 中开发者会建议设置 `requestURI` 这个属性，但是实际在 Zuul 自身的 `PreDecorationFilter` 流程中又会被覆盖一遍。

不过对于一个基于 Servlet 的应用，使用 `HttpServletRequestWrapper` 基本可以解决一切问题，在这个场景中只需要重写其 `getRequestURI` 方法即可。

```java
class RewriteURIRequestWrapper extends HttpServletRequestWrapper {

 private String rewriteURI;

 public RewriteURIRequestWrapper(HttpServletRequest request, String rewriteURI) {
 super(request);
 this.rewriteURI = rewriteURI;
 }

 @Override
 public String getRequestURI() {
 return rewriteURI;
 }

}
```

##  后置过滤器

### 跨域

使用 Gateway 做跨域相比应用本身或是 Nginx 的好处是规则可以配置的更加灵活。例如一个常见的规则。

1.  对于任意的 AJAX 请求，返回 `Access-Control-Allow-Origin` 为 `*`，且 `Access-Control-Allow-Credentials` 为 `true`，这是一个常用的允许任意源跨域的配置，但是不允许请求携带任何 Cookie

2.  如果一个被信任的请求者需要携带 Cookie，那么将它的 `Origin` 增加到白名单中。对于白名单中的请求，返回 `Access-Control-Allow-Origin` 为该域名，且 `Access-Control-Allow-Credentials` 为 `true`，这样请求者可以正常的请求接口，同时可以在请求接口时携带 Cookie

3.  对于 302 的请求，即使在白名单内也必须要设置 `Access-Control-Allow-Origin` 为 `*`，否则重定向后的请求携带的 `Origin` 会为 `null`，有可能会导致 iOS 低版本的某些兼容问题

###  统计

Gateway 可以统一收集所有应用请求的记录，并写入日志文件或是发到监控系统，相比 Nginx 的 access log，好处主要也是二次开发比较方便，比如可以关注一些业务相关的 HTTP 头，或是将请求参数和返回值都保存为日志打入消息队列中，便于线上故障调试。也可以收集一些性能指标发送到类似 Statsd 这样的监控平台。

## 错误过滤器

错误过滤器的主要用法就像是 Jersey 中的 `ExceptionMapper` 或是 Spring MVC 中的 `@ExceptionHandler` 一样，在处理流程中认为有问题时，直接抛出统一的异常，错误过滤器捕获到这个异常后，就可以统一的进行返回值的封装，并直接结束该请求。

#  配置管理

虽然将这些逻辑都切换到了 Gateway，省去了很多维护和迭代的成本，但是也面临着一个很大的问题，就是 Gateway 只有逻辑却没有配置，它并不知道一个请求要走哪些流程。

例如同样是后端服务 API，有的可能是给网页版用的、有的是给客户端用的，亦或是有的给用户用、有的给管理人员用，那么 Gateway 如何知道到底这些 API 是否需要登录、流控以及缓存呢？

理论上我们可以为 Gateway 编写一个管理后台，里面有当前服务的所有 API，每一个开发者都可以在里面创建新的 API，以及为它增加鉴权、缓存、跨域等功能。为了简化使用，也许我们会额外的增加一个权限组，例如 `/admin/*` 下的所有 API 都应该为后台接口，它只允许内部来源的鉴权访问。

但是这样做依旧太复杂了，而且非常硬编码，当开发者开发了一个新的 API 之后，即使这个应用已经能正常接收特定 URI 的请求并处理之后，却还要通过人工的方式去一个管理后台进行额外的配置，而且可能会因为不谨慎打错了路径中的某个单词而造成不必要的事故，这都是不合理的。

我个人推荐的做法是，在后端应用中依旧保持配置的能力，即使应用里已经没有真实处理的逻辑了。例如在 Java 中通过注解声明式的编写 API，且在应用启动时自动注册 Gateway 就是一种比较好的选择。

```java
/**
 * 这个接口需要鉴权，鉴权方式是 OAuth
 */
@Authorization(OAuth)
@RequestMapping(value = "/users/{id}", method = RequestMethod.DELETE)
public void del(@PathVariable int id) {
 //...
}

/**
 * 这个接口可以缓存，并且每个 IP/User 每秒最多请求 10 次
 */
@Cacheable
@RateLimiting(limit = "10/1s", scope = {IP, USER})
@RequestMapping(value = "/users/{id}", method = RequestMethod.GET)
public void info(@PathVariable int id) {
 //...
}
```
这样 API 的编写者就会根据业务场景考虑该 API 需要哪些功能，也减少了管理的复杂度。

除此之外还会有一些后端应用无关的配置，有些是自动化的，例如恶意请求拦截，Gateway 会将所有请求的信息通过消息队列发送给一些实时数据分析的应用，这些应用会对请求分析，发现恶意请求的特征，并通过 Gateway 提供的接口将这些特征上报给 Gateway，Gateway 就可以实时的对这些恶意请求进行拦截。

# 稳定性

在 Nginx 和后端应用之间又建立了一个 Java 应用作为流量入口，很多人会去担心它的稳定性，亦或是担心它能否像 Nginx 一样和后端的多个 upstream 进行交互，以下主要介绍一下 Zuul 的隔离机制以及重试机制。

##  隔离机制

在微服务的模式下，应用之间的联系变得没那么强烈，理想中任何一个应用超过负载或是挂掉了，都不应该去影响到其他应用。但是在 Gateway 这个层面，有没有可能出现一个应用负载过重，导致将整个 Gateway 都压垮了，已致所有应用的流量入口都被切断？

这当然是有可能的，想象一个每秒会接受很多请求的应用，在正常情况下这些请求可能在 10 毫秒之内就能正常响应，但是如果有一天它出了问题，所有请求都会 Block 到 30 秒超时才会断开（例如频繁 Full GC 无法有效释放内存）。那么在这个时候，Gateway 中也会有大量的线程在等待请求的响应，最终会吃光所有线程，导致其他正常应用的请求也受到影响。

在 Zuul 中，每一个后端应用都称为一个 Route，为了避免一个 Route 抢占了太多资源影响到其他 Route 的情况出现，Zuul 使用 Hystrix 对每一个 Route 都做了隔离和限流。

Hystrix 的隔离策略有两种，基于线程或是基于信号量。Zuul 默认的是基于线程的隔离机制，这意味着每一个 Route 的请求都会在一个固定大小且独立的线程池中执行，这样即使其中一个 Route 出现了问题，也只会是某一个线程池发生了阻塞，其他 Route 不会受到影响。

一般使用 Hystrix 时，只有调用量巨大会受到线程开销影响时才会使用信号量进行隔离策略，对于 Zuul 这种网络请求的用途使用线程隔离更加稳妥。

##  重试机制

一般来说，后端应用的健康状态是不稳定的，应用列表随时会有修改，所以 Gateway 必须有足够好的容错机制，能够减少后端应用变更时造成的影响。

Zuul 的路由主要有 Eureka 和 Ribbon 两种方式，下面简单介绍下 Ribbon 支持哪些容错配置。

重试的场景分为三种：

*   `okToRetryOnConnectErrors`：只重试网络错误
*   `okToRetryOnAllErrors`：重试所有错误
*   `OkToRetryOnAllOperations`：重试所有操作（这里不太理解，猜测是 GET/POST 等请求都会重试）

重试的次数有两种：

*   `MaxAutoRetries`：每个节点的最大重试次数
*   `MaxAutoRetriesNextServer`：更换节点重试的最大次数

一般来说我们希望只在网络连接失败时进行重试、或是对 5XX 的 GET 请求进行重试（不推荐对 POST 请求进行重试，无法保证幂等性会造成数据不一致）。单台的重试次数可以尽量小一些，重试的节点数尽量多一些，整体效果会更好。

如果有更加复杂的重试场景，例如需要对特定的某些 API、特定的返回值进行重试，那么也可以通过实现 `RequestSpecificRetryHandler` 定制逻辑（不建议直接使用 `RetryHandler`，因为这个子类可以使用很多已有的功能）。


![image.png](http://upload-images.jianshu.io/upload_images/1233356-37c78728920d8217.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

一个典型的多线程阻塞型架构的运行方式：对于每个请求，由一个专门的线程来进行处理，整个处理流程在线程内是阻塞的。由图可见，当一个请求处理速度很慢（如遇到响应很慢的后段应用），可能会影响整个系统的响应。为了应对这种情况，Netflix也有针对的解决方案：Hystrix。


上面介绍了同步系统的设计，和同步系统设计方式不同，异步系统通常设计成事件驱动。


![image.png](http://upload-images.jianshu.io/upload_images/1233356-693741a4d8224d77.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


如上图所示，当请求到达时，异步系统会将其包装成一个事件，提交到事件循环中。事件循环中会维护一系列的监听器、处理器，针对事件做出一系列的处理，最终将结果返回给用户。这种设计模式通常被称作“反应堆模式（Reactor pattern）”相比于同步多线程系统，异步事件系统可以以较少的线程（甚至是单线程）来处理所有的请求。



# 完整示例

### 网关服务应用( 实现了 API 的代理转发 )：

![image.png](http://upload-images.jianshu.io/upload_images/1233356-254135a2b4f44c16.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

application.properties

```
zuul.routes.api.url=http://localhost:8090

ribbon.eureka.enabled=false

server.port=8080

```

Spring Cloud Zuul will automatically set the path to the application name. In this sample because we set zuul.routes.books.url, so Zuul will proxy requests to /books to this URL.

Notice the second-to-last property in our file: Spring Cloud Netflix Zuul uses Netflix’s Ribbon to perform client-side load balancing, and by default, Ribbon would use Netflix Eureka for service discovery. For this simple example, we’re skipping service discovery, so we’ve set ribbon.eureka.enabled to false. Since Ribbon now can’t use Eureka to look up services, we must specify a url for the Book service.


SimpleFilter   extends ZuulFilter


```java
package hello.filters.pre;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SimpleFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(SimpleFilter.class);

    @Override
    public String filterType() {
        return "前置过滤";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        HttpServletResponse response = ctx.getResponse();

        log.info(String.format("%s request to %s", request.getMethod(), request.getRequestURL().toString()));
        log.info(String.format("response Status : %s, ContentType:  %s", response.getStatus(), response.getContentType()));

        return null;
    }

}

```


GatewayApplication


```java
package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import hello.filters.pre.SimpleFilter;

@EnableZuulProxy
@SpringBootApplication
public class GatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }

  @Bean
  public SimpleFilter simpleFilter() {
    return new SimpleFilter();
  }

}

```

>  注意到： @EnableZuulProxy



### 真正的服务应用


![image.png](http://upload-images.jianshu.io/upload_images/1233356-dbd0f0f3e4daf901.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

application.properties
```
spring.application.name=book

server.port=8090

```
BookApplication

```java
package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class BookApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookApplication.class, args);
    }

    @RequestMapping(value = "/title")
    public String title() {
        return "Spring Boot 2.0 极简教程";
    }

    @RequestMapping(value = "/author")
    public String author() {
        return " 陈光剑";
    }
}


```

直接访问真实服务（server.port=8090）：

http://127.0.0.1:8090/book/info

![image.png](http://upload-images.jianshu.io/upload_images/1233356-440f8372c680efa2.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)



 通过网关 Gateway 代理访问：

http://localhost:8080/api/book/info


![image.png](http://upload-images.jianshu.io/upload_images/1233356-3467c47b6d8ef691.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


这个对于的配置是 gateway 工程里面的 application.properties 配置：

```
zuul.routes.api.url=http://localhost:8090
ribbon.eureka.enabled=false
server.port=8080
```


![image.png](http://upload-images.jianshu.io/upload_images/1233356-ebb06ba4a7dcf12a.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


代理 Filter 日志：

```
2018-01-03 02:09:07.685  INFO 50363 --- [nio-8080-exec-1] o.s.c.n.zuul.web.ZuulHandlerMapping      : Mapped URL path [/api/**] onto handler of type [class org.springframework.cloud.netflix.zuul.web.ZuulController]
2018-01-03 02:09:07.726  INFO 50363 --- [nio-8080-exec-1] hello.filters.pre.SimpleFilter           : GET request to http://localhost:8080/api/book/info
2018-01-03 02:09:07.726  INFO 50363 --- [nio-8080-exec-1] hello.filters.pre.SimpleFilter           : LocalAddr: 0:0:0:0:0:0:0:1
2018-01-03 02:09:07.727  INFO 50363 --- [nio-8080-exec-1] hello.filters.pre.SimpleFilter           : LocalName: localhost
2018-01-03 02:09:07.727  INFO 50363 --- [nio-8080-exec-1] hello.filters.pre.SimpleFilter           : LocalPort: 8080
2018-01-03 02:09:07.727  INFO 50363 --- [nio-8080-exec-1] hello.filters.pre.SimpleFilter           : RemoteAddr: 0:0:0:0:0:0:0:1
2018-01-03 02:09:07.727  INFO 50363 --- [nio-8080-exec-1] hello.filters.pre.SimpleFilter           : RemoteHost: 0:0:0:0:0:0:0:1
2018-01-03 02:09:07.727  INFO 50363 --- [nio-8080-exec-1] hello.filters.pre.SimpleFilter           : RemotePort: 49917

```

其中的核心 Controller 是 ZuulController


```java
package org.springframework.cloud.netflix.zuul.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ServletWrappingController;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.ZuulServlet;

/**
 * @author Spencer Gibb
 */
public class ZuulController extends ServletWrappingController {

	public ZuulController() {
		setServletClass(ZuulServlet.class);
		setServletName("zuul");
		setSupportedMethods((String[]) null); // Allow all
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try {
			return super.handleRequestInternal(request, response);
		}
		finally {
			// @see com.netflix.zuul.context.ContextLifecycleFilter.doFilter
			RequestContext.getCurrentContext().unset();
		}
	}

}

```

其中的核心 ZuulServlet 类代码如下

Core Zuul servlet which intializes and orchestrates zuulFilter execution


```java
/*
 * Copyright 2013 Netflix, Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */
package com.netflix.zuul.http;

import com.netflix.zuul.FilterProcessor;
import com.netflix.zuul.ZuulRunner;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Core Zuul servlet which intializes and orchestrates zuulFilter execution
 *
 * @author Mikey Cohen
 *         Date: 12/23/11
 *         Time: 10:44 AM
 */
public class ZuulServlet extends HttpServlet {

    private static final long serialVersionUID = -3374242278843351500L;
    private ZuulRunner zuulRunner;


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String bufferReqsStr = config.getInitParameter("buffer-requests");
        boolean bufferReqs = bufferReqsStr != null && bufferReqsStr.equals("true") ? true : false;

        zuulRunner = new ZuulRunner(bufferReqs);
    }

    @Override
    public void service(javax.servlet.ServletRequest servletRequest, javax.servlet.ServletResponse servletResponse) throws ServletException, IOException {
        try {
            init((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);

            // Marks this request as having passed through the "Zuul engine", as opposed to servlets
            // explicitly bound in web.xml, for which requests will not have the same data attached
            RequestContext context = RequestContext.getCurrentContext();
            context.setZuulEngineRan();

            try {
                preRoute();
            } catch (ZuulException e) {
                error(e);
                postRoute();
                return;
            }
            try {
                route();
            } catch (ZuulException e) {
                error(e);
                postRoute();
                return;
            }
            try {
                postRoute();
            } catch (ZuulException e) {
                error(e);
                return;
            }

        } catch (Throwable e) {
            error(new ZuulException(e, 500, "UNHANDLED_EXCEPTION_" + e.getClass().getName()));
        } finally {
            RequestContext.getCurrentContext().unset();
        }
    }

    /**
     * executes "post" ZuulFilters
     *
     * @throws ZuulException
     */
    void postRoute() throws ZuulException {
        zuulRunner.postRoute();
    }

    /**
     * executes "route" filters
     *
     * @throws ZuulException
     */
    void route() throws ZuulException {
        zuulRunner.route();
    }

    /**
     * executes "pre" filters
     *
     * @throws ZuulException
     */
    void preRoute() throws ZuulException {
        zuulRunner.preRoute();
    }

    /**
     * initializes request
     *
     * @param servletRequest
     * @param servletResponse
     */
    void init(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        zuulRunner.init(servletRequest, servletResponse);
    }

    /**
     * sets error context info and executes "error" filters
     *
     * @param e
     */
    void error(ZuulException e) {
        RequestContext.getCurrentContext().setThrowable(e);
        zuulRunner.error();
    }

    @RunWith(MockitoJUnitRunner.class)
    public static class UnitTest {

        @Mock
        HttpServletRequest servletRequest;
        @Mock
        HttpServletResponseWrapper servletResponse;
        @Mock
        FilterProcessor processor;
        @Mock
        PrintWriter writer;

        @Before
        public void before() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void testProcessZuulFilter() {

            ZuulServlet zuulServlet = new ZuulServlet();
            zuulServlet = spy(zuulServlet);
            RequestContext context = spy(RequestContext.getCurrentContext());


            try {
                FilterProcessor.setProcessor(processor);
                RequestContext.testSetCurrentContext(context);
                when(servletResponse.getWriter()).thenReturn(writer);

                zuulServlet.init(servletRequest, servletResponse);
                verify(zuulServlet, times(1)).init(servletRequest, servletResponse);
                assertTrue(RequestContext.getCurrentContext().getRequest() instanceof HttpServletRequestWrapper);
                assertTrue(RequestContext.getCurrentContext().getResponse() instanceof HttpServletResponseWrapper);

                zuulServlet.preRoute();
                verify(processor, times(1)).preRoute();

                zuulServlet.postRoute();
                verify(processor, times(1)).postRoute();
//                verify(context, times(1)).unset();

                zuulServlet.route();
                verify(processor, times(1)).route();
                RequestContext.testSetCurrentContext(null);

            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }

}

```


从下面的源代码中，可以看出，前置过滤器的名称是硬编码关键字"pre" ：

![image.png](http://upload-images.jianshu.io/upload_images/1233356-fbcd3d40abca1d44.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


也就是我们在重写 filterType 这个方法时返回的值：

```java
    @Override
    public String filterType() {
        return "pre";
    }
```

我们也可以看到 ZuulServlet 里面的核心实现逻辑：


![image.png](http://upload-images.jianshu.io/upload_images/1233356-c00af90932baf722.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


除了“pre” 路由之外，还有下面的两个 route：

```java

    /**
     * Runs all "route" filters. These filters route calls to an origin.
     *
     * @throws ZuulException if an exception occurs.
     */
    public void route() throws ZuulException {
        try {
            runFilters("route");
        } catch (Throwable e) {
            if (e instanceof ZuulException) {
                throw (ZuulException) e;
            }
            throw new ZuulException(e, 500, "UNCAUGHT_EXCEPTION_IN_ROUTE_FILTER_" + e.getClass().getName());
        }
    }



    /**
     * runs "post" filters which are called after "route" filters. ZuulExceptions from ZuulFilters are thrown.
     * Any other Throwables are caught and a ZuulException is thrown out with a 500 status code
     *
     * @throws ZuulException
     */
    public void postRoute() throws ZuulException {
        try {
            runFilters("post");
        } catch (Throwable e) {
            if (e instanceof ZuulException) {
                throw (ZuulException) e;
            }
            throw new ZuulException(e, 500, "UNCAUGHT_EXCEPTION_IN_POST_FILTER_" + e.getClass().getName());
        }

    }


```



本节示例工程源代码：

API Gateway 网关工程： https://github.com/KotlinSpringBoot/demo3_zuul_api_gateway


微服务提供者工程：

https://github.com/KotlinSpringBoot/demo3_zuul_microservice_provider
