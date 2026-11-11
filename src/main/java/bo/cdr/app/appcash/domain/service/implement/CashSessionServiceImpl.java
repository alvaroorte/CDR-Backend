package bo.cdr.app.appcash.domain.service.implement;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.domain.service.CRUDServiceImpl;
import bo.cdr.app.appcash.data.CashMovementRepository;
import bo.cdr.app.appcash.data.CashSessionRepository;
import bo.cdr.app.appcash.domain.entity.CashMovement;
import bo.cdr.app.appcash.domain.entity.CashSession;
import bo.cdr.app.appcash.domain.mapper.CashSessionMapper;
import bo.cdr.app.appcash.domain.service.interfaces.CashSessionService;
import bo.cdr.app.appcash.presentation.request.dto.CashMovementDto;
import bo.cdr.app.appcash.presentation.request.dto.CashSessionOpenDto;
import bo.cdr.app.appcash.presentation.response.pojo.CashSessionPojo;
import bo.cdr.app.appsecurity.auth.jwt.JwtService;
import bo.cdr.app.appsecurity.domain.service.interfaces.UserService;
import bo.cdr.app.common.MoneyUtil;
import bo.cdr.app.common.enums.CashMovementTypeEnum;
import bo.cdr.app.common.enums.CashSessionStatusEnum;
import bo.cdr.app.exception.ConflictException;
import bo.cdr.app.exception.response.ValidationErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Caja por usuario (skill cdr-order-management §10). Etapa 3: apertura, movimientos manuales y
 * consulta de la caja abierta. El cierre con arqueo es de la Etapa 5.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashSessionServiceImpl extends CRUDServiceImpl<CashSession, Integer> implements CashSessionService {

    private final CashSessionRepository cashSessionRepository;
    private final CashMovementRepository cashMovementRepository;
    private final CashSessionMapper cashSessionMapper;
    private final UserService userService;
    private final JwtService jwtService;
    private final Clock clock;

    @Override
    protected GenericRepository<CashSession, Integer> getRepository() {
        return cashSessionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CashSessionPojo> getCurrent() {
        return cashSessionRepository.findOpenByUserId(jwtService.getCurrentUserId()).map(this::toPojo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CashSessionPojo open(CashSessionOpenDto dto) {
        Integer userId = jwtService.getCurrentUserId();
        if (cashSessionRepository.findOpenByUserId(userId).isPresent()) {
            throw new ConflictException("Ya tiene una caja abierta.");
        }
        CashSession session = new CashSession();
        session.setUser(userService.getById(userId));
        session.setStatus(CashSessionStatusEnum.OPEN);
        session.setOpeningAmount(MoneyUtil.round(dto.getOpeningAmount()));
        session.setOpenedAt(LocalDateTime.now(clock));
        // saveAndFlush: el índice ux_cash_sessions_user_open resuelve dos aperturas simultáneas (409)
        cashSessionRepository.saveAndFlush(session);

        CashMovement opening = new CashMovement();
        opening.setCashSession(session);
        opening.setType(CashMovementTypeEnum.OPENING);
        opening.setAmount(session.getOpeningAmount());
        opening.setDescription("Apertura de caja");
        cashMovementRepository.save(opening);

        log.info("Caja {} abierta por {} con Bs {}", session.getId(), jwtService.getCurrentUsername(),
                session.getOpeningAmount());
        return toPojo(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CashSessionPojo registerMovement(CashMovementDto dto) {
        CashMovementTypeEnum type = CashMovementTypeEnum.fromString(dto.getType());
        if (type != CashMovementTypeEnum.INCOME && type != CashMovementTypeEnum.EXPENSE) {
            throw new ValidationErrorException("Solo se pueden registrar ingresos o egresos manuales.");
        }
        CashSession session = getOpenSessionOfCurrentUser();

        CashMovement movement = new CashMovement();
        movement.setCashSession(session);
        movement.setType(type);
        movement.setAmount(MoneyUtil.round(dto.getAmount()));
        movement.setDescription(dto.getDescription().trim());
        cashMovementRepository.save(movement);

        log.info("Caja {}: {} de Bs {} ({})", session.getId(), type, movement.getAmount(), movement.getDescription());
        return toPojo(session);
    }

    @Override
    @Transactional(readOnly = true)
    public CashSession getOpenSessionOfCurrentUser() {
        return cashSessionRepository.findOpenByUserId(jwtService.getCurrentUserId())
                .orElseThrow(() -> new ValidationErrorException("Debe abrir caja antes de registrar cobros o movimientos."));
    }

    private CashSessionPojo toPojo(CashSession session) {
        return cashSessionMapper.toPojo(session, cashMovementRepository.findPojoByCashSessionId(session.getId()));
    }
}
