import java.util.Random;
/**
 *
 * @author ESCINF
 */

class Mavenproject2 {

    // Clase para encapsular las estadísticas
    public static class Resultado {
        long totalNumeros = 0;
        long suma = 0;
        int minimo = 100;
        int maximo = 90;

        double getPromedio() {
            return (totalNumeros == 0) ? 0.0 : (double) suma / totalNumeros;
        }
    }

    /**
     * Genera un arreglo de n enteros aleatorios en el rango [minValor, maxValor].
     */
    public static int[] generarDatos(int n, int minValor, int maxValor) {
        int[] datos = new int[n];
        Random random = new Random();

        int rango = maxValor - minValor + 1;

        for (int i = 0; i < n; i++) {
            datos[i] = minValor + random.nextInt(rango);
        }

        return datos;
    }

    /**
     * Procesa el arreglo de forma secuencial y calcula las estadísticas.
     */
    public static Resultado procesar(int[] datos) {
        Resultado res = new Resultado();

        for (int valor : datos) {
            res.totalNumeros++;
            res.suma += valor;

            if (valor < res.minimo) res.minimo = valor;
            if (valor > res.maximo) res.maximo = valor;
        }

        return res;
    }

    public static void main(String[] args) {

        int n = 1_000_000; 

        if (args.length >= 1) {
            try {
                n = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Argumento inválido, usando tamaño por defecto: " + n);
            }
        }

        int minValor = -1_000_000;
        int maxValor = 1_000_000;

        System.out.println("Generando " + n + " números aleatorios entre " +
                minValor + " y " + maxValor + "...");

        // Generación de datos
        int[] datos = generarDatos(n, minValor, maxValor);

        // Medición del tiempo de procesamiento secuencial
        long inicio = System.nanoTime();
        Resultado r = procesar(datos);
        long fin = System.nanoTime();

        double tiempoMs = (fin - inicio) / 1_000_000.0;

        System.out.println("=== RESULTADOS SECUENCIALES ===");
        System.out.println("Total de números: " + r.totalNumeros);
        System.out.println("Suma total: " + r.suma);
        System.out.println("Promedio: " + r.getPromedio());
        System.out.println("Valor mínimo: " + r.minimo);
        System.out.println("Valor máximo: " + r.maximo);
        System.out.println("Tiempo total de procesamiento: " + tiempoMs + " ms");
    }
}