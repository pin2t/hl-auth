import java.util.*;

enum Country {
    NO(""),
    PAPUA_NEW_GUINEA("Papua New Guinea"),
    CAMBODIA("Cambodia"),
    KAZAKHSTAN("Kazakhstan"),
    PARAGUAY("Paraguay"),
    SYRIA("Syria"),
    ALAND_ISLANDS("Åland Islands"),
    SOLOMON_ISLANDS("Solomon Islands"),
    BAHAMAS("Bahamas"),
    MONTSERRAT("Montserrat"),
    MARSHALL_ISLANDS("Marshall Islands"),
    MALI("Mali"),
    US_OUTLYING_ISLANDS("U.S. Outlying Islands"),
    GUADELOUPE("Guadeloupe"),
    PANAMA("Panama"),
    LAOS("Laos"),
    ARGENTINA("Argentina"),
    FALKLAND_ISLANDS("Falkland Islands"),
    SEYCHELLES("Seychelles"),
    ZAMBIA("Zambia"),
    BELIZE("Belize"),
    STKITTSANDNEVIS("St Kitts and Nevis"),
    BAHRAIN("Bahrain"),
    GUINEABISSAU("Guinea-Bissau"),
    NAMIBIA("Namibia"),
    FINLAND("Finland"),
    COMOROS("Comoros"),
    FAROEISLANDS("Faroe Islands"),
    NORTHMACEDONIA("North Macedonia"),
    GEORGIA("Georgia"),
    TURKIYE("Türkiye"),
    YEMEN("Yemen"),
    ERITREA("Eritrea"),
    PUERTORICO("Puerto Rico"),
    MADAGASCAR("Madagascar"),
    ARUBA("Aruba"),
    IVORYCOAST("Ivory Coast"),
    LIBYA("Libya"),
    SVALBARDANDJANMAYEN("Svalbard and Jan Mayen"),
    SWEDEN("Sweden"),
    SOUTHGEORGIAANDTHESOUTHSANDWICHISLANDS("South Georgia and the South Sandwich Islands"),
    SAINTMARTIN("Saint Martin"),
    MALAWI("Malawi"),
    COCOSKEELINGISLANDS("Cocos (Keeling), Islands"),
    ANDORRA("Andorra"),
    LIECHTENSTEIN("Liechtenstein"),
    POLAND("Poland"),
    JORDAN("Jordan"),
    BULGARIA("Bulgaria"),
    TUNISIA("Tunisia"),
    BONAIRESINTEUSTATIUSANDSABA("Bonaire, Sint Eustatius, and Saba"),
    UNITEDARABEMIRATES("United Arab Emirates"),
    TUVALU("Tuvalu"),
    KENYA("Kenya"),
    FRENCHPOLYNESIA("French Polynesia"),
    DJIBOUTI("Djibouti"),
    LEBANON("Lebanon"),
    BRUNEI("Brunei"),
    AZERBAIJAN("Azerbaijan"),
    CUBA("Cuba"),
    MAURITANIA("Mauritania"),
    SAINTLUCIA("Saint Lucia"),
    GUERNSEY("Guernsey"),
    MAYOTTE("Mayotte"),
    ISRAEL("Israel"),
    SANMARINO("San Marino"),
    AUSTRALIA("Australia"),
    TAJIKISTAN("Tajikistan"),
    MYANMAR("Myanmar"),
    CAMEROON("Cameroon"),
    GIBRALTAR("Gibraltar"),
    CYPRUS("Cyprus"),
    NORTHERNMARIANAISLANDS("Northern Mariana Islands"),
    MALAYSIA("Malaysia"),
    OMAN("Oman"),
    ICELAND("Iceland"),
    ARMENIA("Armenia"),
    GABON("Gabon"),
    LUXEMBOURG("Luxembourg"),
    BRAZIL("Brazil"),
    TURKSANDCAICOSISLANDS("Turks and Caicos Islands"),
    ALGERIA("Algeria"),
    JERSEY("Jersey"),
    SLOVENIA("Slovenia"),
    CABOVERDE("Cabo Verde"),
    ANTIGUAANDBARBUDA("Antigua and Barbuda"),
    ECUADOR("Ecuador"),
    COLOMBIA("Colombia"),
    MOLDOVA("Moldova"),
    VANUATU("Vanuatu"),
    ESWATINI("Eswatini"),
    ITALY("Italy"),
    HONDURAS("Honduras"),
    ANTARCTICA("Antarctica"),
    NAURU("Nauru"),
    HAITI("Haiti"),
    BURUNDI("Burundi"),
    AFGHANISTAN("Afghanistan"),
    SINGAPORE("Singapore"),
    FRENCHGUIANA("French Guiana"),
    FEDERATEDSTATESOFMICRONESIA("Federated States of Micronesia"),
    CHRISTMASISLAND("Christmas Island"),
    AMERICANSAMOA("American Samoa"),
    VATICANCITY("Vatican City"),
    RUSSIA("Russia"),
    CHINA("China"),
    MARTINIQUE("Martinique"),
    SINTMAARTEN("Sint Maarten"),
    SAINTPIERREANDMIQUELON("Saint Pierre and Miquelon"),
    KYRGYZSTAN("Kyrgyzstan"),
    BHUTAN("Bhutan"),
    ROMANIA("Romania"),
    TOGO("Togo"),
    PHILIPPINES("Philippines"),
    UZBEKISTAN("Uzbekistan"),
    BRITISHVIRGINISLANDS("British Virgin Islands"),
    ZIMBABWE("Zimbabwe"),
    BRITISHINDIANOCEANTERRITORY("British Indian Ocean Territory"),
    MONTENEGRO("Montenegro"),
    INDONESIA("Indonesia"),
    DOMINICA("Dominica"),
    BENIN("Benin"),
    ANGOLA("Angola"),
    SUDAN("Sudan"),
    PORTUGAL("Portugal"),
    NEWCALEDONIA("New Caledonia"),
    NORTHKOREA("North Korea"),
    GRENADA("Grenada"),
    GREECE("Greece"),
    CAYMANISLANDS("Cayman Islands"),
    LATVIA("Latvia"),
    MONGOLIA("Mongolia"),
    IRAN("Iran"),
    MOROCCO("Morocco"),
    GUYANA("Guyana"),
    GUATEMALA("Guatemala"),
    IRAQ("Iraq"),
    CHILE("Chile"),
    NEPAL("Nepal"),
    ISLEOFMAN("Isle of Man"),
    TANZANIA("Tanzania"),
    UKRAINE("Ukraine"),
    GHANA("Ghana"),
    CURACAO("Curaçao"),
    ANGUILLA("Anguilla"),
    INDIA("India"),
    CANADA("Canada"),
    MALDIVES("Maldives"),
    BELGIUM("Belgium"),
    TAIWAN("Taiwan"),
    SOUTHAFRICA("South Africa"),
    TRINIDADANDTOBAGO("Trinidad and Tobago"),
    HEARDANDMCDONALDISLANDS("Heard and McDonald Islands"),
    BERMUDA("Bermuda"),
    CENTRALAFRICANREPUBLIC("Central African Republic"),
    JAMAICA("Jamaica"),
    TURKMENISTAN("Turkmenistan"),
    PERU("Peru"),
    GERMANY("Germany"),
    FIJI("Fiji"),
    TOKELAU("Tokelau"),
    HONGKONG("Hong Kong"),
    GUINEA("Guinea"),
    UNITEDSTATES("United States"),
    SOMALIA("Somalia"),
    CHAD("Chad"),
    PITCAIRNISLANDS("Pitcairn Islands"),
    THAILAND("Thailand"),
    KIRIBATI("Kiribati"),
    EQUATORIALGUINEA("Equatorial Guinea"),
    COSTARICA("Costa Rica"),
    VIETNAM("Vietnam"),
    SAOTOMEANDPRINCIPE("São Tomé and Príncipe"),
    KUWAIT("Kuwait"),
    NIGERIA("Nigeria"),
    CONGOREPUBLIC("Congo Republic"),
    CROATIA("Croatia"),
    SRILANKA("Sri Lanka"),
    COOKISLANDS("Cook Islands"),
    URUGUAY("Uruguay"),
    TIMORLESTE("Timor-Leste"),
    UNITEDKINGDOM("United Kingdom"),
    SWITZERLAND("Switzerland"),
    SAMOA("Samoa"),
    PALESTINE("Palestine"),
    SPAIN("Spain"),
    LIBERIA("Liberia"),
    VENEZUELA("Venezuela"),
    BURKINAFASO("Burkina Faso"),
    PALAU("Palau"),
    SAINTBARTHELEMY("Saint Barthélemy"),
    ESTONIA("Estonia"),
    THENETHERLANDS("The Netherlands"),
    WALLISANDFUTUNA("Wallis and Futuna"),
    NIUE("Niue"),
    SOUTHKOREA("South Korea"),
    AUSTRIA("Austria"),
    MOZAMBIQUE("Mozambique"),
    ELSALVADOR("El Salvador"),
    MONACO("Monaco"),
    GUAM("Guam"),
    LESOTHO("Lesotho"),
    TONGA("Tonga"),
    WESTERNSAHARA("Western Sahara"),
    SOUTHSUDAN("South Sudan"),
    HUNGARY("Hungary"),
    REUNION("Réunion"),
    JAPAN("Japan"),
    BELARUS("Belarus"),
    MAURITIUS("Mauritius"),
    BOUVETISLAND("Bouvet Island"),
    ALBANIA("Albania"),
    NORFOLKISLAND("Norfolk Island"),
    NEWZEALAND("New Zealand"),
    SENEGAL("Senegal"),
    DRCONGO("DR Congo"),
    ETHIOPIA("Ethiopia"),
    CZECHIA("Czechia"),
    EGYPT("Egypt"),
    SIERRALEONE("Sierra Leone"),
    BOLIVIA("Bolivia"),
    MALTA("Malta"),
    SAUDIARABIA("Saudi Arabia"),
    PAKISTAN("Pakistan"),
    KOSOVO("Kosovo"),
    GAMBIA("Gambia"),
    QATAR("Qatar"),
    IRELAND("Ireland"),
    USVIRGINISLANDS("U.S. Virgin Islands"),
    SLOVAKIA("Slovakia"),
    LITHUANIA("Lithuania"),
    SERBIA("Serbia"),
    FRANCE("France"),
    BOSNIAANDHERZEGOVINA("Bosnia and Herzegovina"),
    NIGER("Niger"),
    RWANDA("Rwanda"),
    FRENCHSOUTHERNTERRITORIES("French Southern Territories"),
    STVINCENTANDGRENADINES("St Vincent and Grenadines"),
    BANGLADESH("Bangladesh"),
    BARBADOS("Barbados"),
    NICARAGUA("Nicaragua"),
    NORWAY("Norway"),
    BOTSWANA("Botswana"),
    MACAO("Macao"),
    DENMARK("Denmark"),
    DOMINICANREPUBLIC("Dominican Republic"),
    UGANDA("Uganda"),
    MEXICO("Mexico"),
    SURINAME("Suriname"),
    SAINTHELENA("Saint Helena"),
    GREENLAND("Greenland");

    static Map<String, Country> vals = new HashMap<>();
    static {
        for (var v : Country.values()) {
            vals.put(v.name, v);
        }
    }

    final String name;

    Country(String name) {
        this.name = name;
    }

    static Country fromName(String name) {
        return vals.getOrDefault(name, Country.NO);
    }
}
