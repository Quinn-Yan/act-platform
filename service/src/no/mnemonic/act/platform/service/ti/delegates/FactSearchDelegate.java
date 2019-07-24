package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchFactRequest;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.handlers.FactSearchHandler;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;
import java.util.function.Function;

public class FactSearchDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final Function<SearchFactRequest, FactSearchCriteria> requestConverter;
  private final FactSearchHandler factSearchHandler;

  @Inject
  public FactSearchDelegate(TiSecurityContext securityContext,
                            Function<SearchFactRequest, FactSearchCriteria> requestConverter,
                            FactSearchHandler factSearchHandler) {
    this.securityContext = securityContext;
    this.requestConverter = requestConverter;
    this.factSearchHandler = factSearchHandler;
  }

  public ResultSet<Fact> handle(SearchFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.viewFactObjects);
    return factSearchHandler.search(requestConverter.apply(request), request.getIncludeRetracted());
  }
}
