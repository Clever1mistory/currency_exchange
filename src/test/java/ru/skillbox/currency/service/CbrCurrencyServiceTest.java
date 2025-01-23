package ru.skillbox.currency.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import ru.skillbox.currency.exchange.entity.Currency;
import ru.skillbox.currency.exchange.entity.Valute;
import ru.skillbox.currency.exchange.mapper.CurrencyMapper;
import ru.skillbox.currency.exchange.repository.CurrencyRepository;
import ru.skillbox.currency.exchange.service.CbrCurrencyService;
import ru.skillbox.currency.exchange.xmlparser.ValCurs;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.net.URL;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


class CbrCurrencyServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private CurrencyMapper currencyMapper;

    @InjectMocks
    private CbrCurrencyService cbrCurrencyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(cbrCurrencyService, "cbrCurrencyUrl", "http://www.cbr.ru/scripts/XML_daily.asp");
    }

    @Test
    void testUpdateCurrencies() throws Exception {
        JAXBContext context = mock(JAXBContext.class);
        Unmarshaller unmarshaller = mock(Unmarshaller.class);

        ValCurs valCurs = new ValCurs();
        Valute valute = new Valute();
        valute.setCharCode("USD");
        valute.setName("Доллар США");
        valute.setNominal("1");
        valute.setValue("93.5");
        valute.setNumCode("840");
        valCurs.setValute(Collections.singletonList(valute));

        try (MockedStatic<JAXBContext> mockedJAXBContext = mockStatic(JAXBContext.class)) {
            mockedJAXBContext.when(() -> JAXBContext.newInstance(ValCurs.class)).thenReturn(context);

            when(context.createUnmarshaller()).thenReturn(unmarshaller);
            when(unmarshaller.unmarshal(any(URL.class))).thenReturn(valCurs);

            when(currencyRepository.findByIsoCharCode(anyString())).thenReturn(null);

            cbrCurrencyService.updateCurrencies();

            verify(currencyRepository, times(1)).findByIsoCharCode("USD");

            // Используем ArgumentCaptor для захвата объекта Currency, переданного в save
            ArgumentCaptor<Currency> currencyCaptor = ArgumentCaptor.forClass(Currency.class);
            verify(currencyRepository, times(1)).save(currencyCaptor.capture());

            Currency savedCurrency = currencyCaptor.getValue();
            assertEquals("Доллар США", savedCurrency.getName());
            assertEquals(1L, savedCurrency.getNominal());
            assertEquals(93.5, savedCurrency.getValue());
            assertEquals(840L, savedCurrency.getIsoNumCode());
            assertEquals("USD", savedCurrency.getIsoCharCode());
        }
    }
}
