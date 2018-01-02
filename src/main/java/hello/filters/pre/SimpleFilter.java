package hello.filters.pre;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

public class SimpleFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(SimpleFilter.class);

    /**
     * public void testFilterType() {
     * assertThat(filter.filterType()).isEqualTo("pre");
     * }
     *
     * @return
     */

    @Override
    public String filterType() {
        return "pre";
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

        log.info(String.format("%s request to %s", request.getMethod(), request.getRequestURL().toString()));
        log.info(String.format("LocalAddr: %s", request.getLocalAddr()));
        log.info(String.format("LocalName: %s", request.getLocalName()));
        log.info(String.format("LocalPort: %s", request.getLocalPort()));

        log.info(String.format("RemoteAddr: %s", request.getRemoteAddr()));
        log.info(String.format("RemoteHost: %s", request.getRemoteHost()));
        log.info(String.format("RemotePort: %s", request.getRemotePort()));

        return null;
    }

}
