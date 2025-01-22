package ru.skillbox.currency.exchange.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.skillbox.currency.exchange.entity.Currency;
import ru.skillbox.currency.exchange.entity.Valute;
import ru.skillbox.currency.exchange.mapper.CurrencyMapper;
import ru.skillbox.currency.exchange.repository.CurrencyRepository;
import ru.skillbox.currency.exchange.xmlparser.ValCurs;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.net.URL;

@Service
@Slf4j
@RequiredArgsConstructor
public class CbrCurrencyService {

    private final CurrencyRepository currencyRepository;
    private final CurrencyMapper currencyMapper;

    @Value("${cbr.currency.url}")
    private String cbrCurrencyUrl;

    @Scheduled(fixedRate = 3600000)
    public void updateCurrencies() {
        log.info("Starting currency update...");
        try {
            URL url = new URL(cbrCurrencyUrl);
            JAXBContext context = JAXBContext.newInstance(ValCurs.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            ValCurs valCurs = (ValCurs) unmarshaller.unmarshal(url);

            for (Valute valute : valCurs.getValute()) {
                Currency currency = currencyRepository.findByIsoCharCode(valute.getCharCode());
                if (currency == null) {
                    currency = new Currency();
                }
                currency.setName(valute.getName());
                currency.setNominal(Long.parseLong(valute.getNominal()));
                currency.setValue(Double.parseDouble(valute.getValue().replace(",", ".")));
                currency.setIsoNumCode(Long.parseLong(valute.getNumCode()));
                currency.setIsoCharCode(valute.getCharCode());

                currencyRepository.save(currency);
            }

            log.info("Currencies updated successfully");
        } catch (Exception e) {
            log.error("Error updating currencies", e);
        }
    }
}
