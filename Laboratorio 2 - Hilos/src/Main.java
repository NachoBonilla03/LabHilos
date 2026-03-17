import java.util.Random;

class Paralel{

    /**
     * Clase para encapsular las estadísticas
     */
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
     * * Clase trabajadora que procesa una sección especifica del arreglo.
     * */
    public static class Worker implements Runnable {
        private final int[] datos;
        private final int inicio;
        private final int fin;
        private Resultado resultadoParcial;

        public Worker(int[] datos, int inicio, int fin) {
            this.datos = datos;
            this.inicio = inicio;
            this.fin = fin;
        }

        @Override
        public void run() {
            Resultado res = new Resultado();
            for (int i = inicio; i < fin; i++) {
                res.totalNumeros += 1;
                res.suma += datos[i];

                if (datos[i] < res.minimo) { res.minimo = datos[i]; }
                if (datos[i] > res.maximo) { res.maximo = datos[i]; }
            }
            this.resultadoParcial = res;
        }

        public Resultado getResultadoParcial() {
            return resultadoParcial;
        }
    }

    /**
     * * Procesa el arreglo de forma secuencial y calcula las estadísticas.
     * */
    public static Resultado procesarSecuencial(int[] datos) {
        Resultado res = new Resultado();

        for (int valor : datos) {
            res.totalNumeros++;
            res.suma += valor;

            if (valor < res.minimo) res.minimo = valor;
            if (valor > res.maximo) res.maximo = valor;
        }

        return res;
    }

    /**
     * * Procesa el arreglo de forma paralela y calcula las estadísticas.
     * */
    public static Resultado procesarParalelo(int[] datos, int cantidadHilos) throws InterruptedException {
        if (cantidadHilos <= 0) {
            throw new IllegalArgumentException("La cantidad de hilos debe ser mayor que 0.");
        }

        Thread[] threads = new Thread[cantidadHilos];
        Worker[] workers = new Worker[cantidadHilos];
        int n = datos.length;

        for (int i = 0; i < cantidadHilos; i++) {
            int inicio = i * n / cantidadHilos;
            int fin = (i + 1) * n / cantidadHilos;

            workers[i] = new Worker(datos, inicio, fin);
            threads[i] = new Thread(workers[i], "Worker-" + i);
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Resultado total = new Resultado();
        for (Worker worker : workers) {
            Resultado parcial = worker.getResultadoParcial();
            if (parcial != null && parcial.totalNumeros > 0) {
                total.totalNumeros += parcial.totalNumeros;
                total.suma += parcial.suma;
                total.minimo = Math.min(total.minimo, parcial.minimo);
                total.maximo = Math.max(total.maximo, parcial.maximo);
            }
        }


        return total;
    }

    public static int[] parsearHilos(String texto) {
        String[] partes = texto.split(",");
        int[] hilos = new int[partes.length];

        for (int i = 0; i < partes.length; i++) {
            hilos[i] = Integer.parseInt(partes[i].trim());
        }

        return hilos;
    }

    public static void main(String[] args) {
        int n = 1_000_000;
        int[] configuracionHilos = {2, 4, 8};

        if (args.length >= 1) {
            try {
                n = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Argumento inválido para n. Se usará el valor por defecto: " + n);
            }
        }

        if (args.length >= 2) {
            try {
                configuracionHilos = parsearHilos(args[1]);
            } catch (Exception e) {
                System.out.println("Argumento inválido para la lista de hilos. Se usará 2,4,8.");
                configuracionHilos = new int[]{2, 4, 8};
            }
        }

        int minValor = -1_000_000;
        int maxValor = 1_000_000;

        System.out.println("\nGenerando " + n + " números aleatorios entre " +
                minValor + " y " + maxValor + "...");

        // Generación de datos
        int[] datos = generarDatos(n, minValor, maxValor);

        // Medición del tiempo de procesamiento secuencial
        long inicio = System.nanoTime();
        Resultado resultadoSec = procesarSecuencial(datos);
        long fin = System.nanoTime();
        double tiempoSecMs = (fin - inicio) / 1_000_000.0;

        System.out.println("\n=== RESULTADOS SECUENCIALES ===");
        System.out.printf(
                "%-15s %-15s %-12s %-12s %-12s %-12s%n",
                "TotalNumeros",
                "SumaTotal",
                "Promedio",
                "Minimo",
                "Maximo",
                "Tiempo(ms)"
        );

        System.out.printf(
                "%-15d %-15d %-12.3f %-12d %-12d %-12.3f%n",
                resultadoSec.totalNumeros,
                resultadoSec.suma,
                resultadoSec.getPromedio(),
                resultadoSec.minimo,
                resultadoSec.maximo,
                tiempoSecMs
        );

        System.out.println("\n=== RESULTADOS PARALELOS ===");
        System.out.printf(
                "%-7s %-15s %-15s %-12s %-12s %-12s %-12s %-10s%n",
                "Hilos",
                "TotalNumeros",
                "SumaTotal",
                "Promedio",
                "Minimo",
                "Maximo",
                "Tiempo(ms)",
                "Speedup"
        );

        for (int h : configuracionHilos) {
            try {

                long inicioPar = System.nanoTime();
                Resultado resultadoPar = procesarParalelo(datos, h);
                long finPar = System.nanoTime();
                double tiempoParMs = (finPar - inicioPar) / 1_000_000.0;
                double speedup = tiempoSecMs / tiempoParMs;

                System.out.printf(
                        "%-7d %-15d %-15d %-12.3f %-12d %-12d %-12.3f %-10.3f%n",
                        h,
                        resultadoPar.totalNumeros,
                        resultadoPar.suma,
                        resultadoPar.getPromedio(),
                        resultadoPar.minimo,
                        resultadoPar.maximo,
                        tiempoParMs,
                        speedup
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Se interrumpió la ejecución con " + h + " hilos.");
            } catch (IllegalArgumentException e) {
                System.out.println("Configuración inválida de hilos (" + h + "): " + e.getMessage());
            }
        }
    }
}