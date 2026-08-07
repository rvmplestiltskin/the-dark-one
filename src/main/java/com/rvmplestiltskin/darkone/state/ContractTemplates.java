package com.rvmplestiltskin.darkone.state;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pre-written contract templates. Always worded to favor the Dark One.
 */
public final class ContractTemplates {

    private ContractTemplates() {}

    public static final Map<String, String> TEMPLATES = new LinkedHashMap<>();

    static {
        TEMPLATES.put("servicio",
                "El abajo firmante se compromete a prestar servicio leal e incondicional al Oscuro " +
                "por el resto de su existencia en este reino. A cambio, el Oscuro otorga proteccion " +
                "contra amenazas ordinarias y un favor menor a discrecion propia. El firmante reconoce " +
                "que dicho favor no incluye la cesion del poder del Oscuro, la daga, ni inmunidad " +
                "frente a la voluntad del Oscuro. Cualquier intento de traicion, desobediencia o " +
                "omision deliberada se interpretara como incumplimiento total, quedando el alma del " +
                "firmante en deposito del Oscuro hasta que este considere saldada la deuda, sin " +
                "plazo maximo ni derecho a reclamacion.");

        TEMPLATES.put("deuda",
                "El abajo firmante declara deber al Oscuro una deuda de naturaleza no especificada " +
                "en este acto, cuyo monto, forma y momento de cobro seran determinados unicamente " +
                "por el Oscuro. A cambio de posponer el cobro, el firmante cede al Oscuro el derecho " +
                "de exigir un servicio, objeto o secreto de valor equivalente en cualquier momento. " +
                "El firmante renuncia a cuestionar la equivalencia del cobro. Si el firmante fallece " +
                "antes del cobro, la deuda se transmite a su heredero mas cercano presente en el " +
                "reino, o en su defecto queda vinculada a su nombre hasta que el Oscuro la declare " +
                "extinguida. Este contrato no puede rescindirse por acuerdo mutuo salvo que el " +
                "Oscuro lo declare por escrito.");

        TEMPLATES.put("nombre",
                "El abajo firmante entrega voluntariamente al Oscuro el derecho de uso, invocacion " +
                "y dominio simbolico sobre su nombre verdadero en este reino. El Oscuro podra " +
                "emplear dicho nombre para localizar, convocar o influir al firmante una vez por " +
                "ciclo lunar. A cambio, el firmante recibe el silencio del Oscuro respecto a un " +
                "secreto que el firmante declare en el acto de la firma (o, si no declara ninguno, " +
                "respecto a su ubicacion durante tres dias). El firmante no podra cambiar de nombre " +
                "con el fin de eludir este contrato; cualquier alias nuevo quedara tambien sujeto " +
                "a la misma cesion. La revocacion solo procede si el Oscuro lo concede de forma " +
                "explicita y unilateral.");

        TEMPLATES.put("proteccion",
                "El Oscuro se compromete a interponer su poder una unica vez para salvar la vida " +
                "del firmante ante muerte inminente causada por terceros, siempre que el firmante " +
                "invoque el nombre del Oscuro en voz alta y el Oscuro se encuentre en el mismo " +
                "reino. A cambio, el firmante cede al Oscuro: (1) la primicia de todo botin " +
                "relevante obtenido en la semana siguiente a la proteccion; (2) la obligacion de " +
                "responder a un llamado del Oscuro sin demora injustificada; y (3) el reconocimiento " +
                "de que la proteccion no cubre suicidio, traicion al Oscuro, ni danios causados " +
                "por la daga del Oscuro. Si el firmante sobrevive sin invocar la proteccion, la " +
                "deuda de servicio permanece igualmente vigente por un ciclo lunar completo.");

        TEMPLATES.put("secreto",
                "El abajo firmante entrega al Oscuro un secreto de peso (o se compromete a " +
                "entregarlo en el plazo de tres dias). El Oscuro se obliga a no revelarlo a " +
                "terceros salvo que el firmante incumpla cualquier otra obligacion con el Oscuro. " +
                "A cambio del resguardo, el firmante acepta que el Oscuro podra usar el conocimiento " +
                "del secreto para beneficio propio en tratos futuros, sin compensacion adicional. " +
                "Si el secreto resulta falso, incompleto o ya de dominio publico, el firmante " +
                "debera sustituirlo por otro de igual o mayor valor a juicio exclusivo del Oscuro, " +
                "o prestar un servicio de tres dias bajo mando directo. Este pacto es perpetuo.");

        TEMPLATES.put("territorio",
                "El abajo firmante reconoce la autoridad del Oscuro sobre el territorio que " +
                "habita o administra, en la medida en que no contradiga leyes superiores del " +
                "reino. El firmante permitira paso libre al Oscuro y a quien este designe, " +
                "y no ocultaara informacion sobre amenazas a los intereses del Oscuro en dicha " +
                "zona. A cambio, el Oscuro se abstiene de ejercer hostilidad directa contra el " +
                "firmante mientras el pacto se cumpla. El incumplimiento autoriza al Oscuro a " +
                "considerar nula toda proteccion y a exigir compensacion en bienes o servicio " +
                "sin limite prefijado. La duracion es indefinida hasta renuncia unilateral del Oscuro.");

        TEMPLATES.put("aprendizaje",
                "El Oscuro acepta transmitir al firmante un conocimiento, tecnica o revelacion " +
                "de naturaleza magica o estrategica, en el grado que el Oscuro estime suficiente. " +
                "El firmante pagara con: (1) lealtad de palabra y obra durante el aprendizaje; " +
                "(2) la cesion de cualquier descubrimiento posterior derivado de lo ensenado, " +
                "que pasara a ser del Oscuro en primer derecho; y (3) la prohibicion de ensenar " +
                "a terceros lo recibido sin permiso escrito del Oscuro. Si el firmante abandona " +
                "el aprendizaje o lo usa contra el Oscuro, todo lo aprendido se considerara " +
                "deuda impaga convertible en servicio forzoso hasta que el Oscuro declare " +
                "el equilibrio restaurado.");

        TEMPLATES.put("primera_opcion",
                "El abajo firmante otorga al Oscuro derecho de primera opcion sobre cualquier " +
                "bien, artefacto, informacion o alianza de valor extraordinario que el firmante " +
                "obtenga o este a punto de obtener. El Oscuro dispondra de un dia completo para " +
                "aceptar o rechazar; el silencio se interpreta como rechazo. El precio lo fija " +
                "el Oscuro en terminos que considere justos, sin obligacion de equivalencia " +
                "mercantil. A cambio, el firmante recibe la consideracion del Oscuro como " +
                "interlocutor preferente en un futuro trato de menor cuantia. Eludir este derecho " +
                "mediante cesion a terceros anula beneficios previos y genera deuda de servicio " +
                "inmediata.");
    }

    public static String get(String id) {
        return TEMPLATES.get(id.toLowerCase());
    }

    public static String listIds() {
        return String.join(", ", TEMPLATES.keySet());
    }
}
