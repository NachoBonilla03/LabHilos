import java.util.Arrays;
import java.util.Random;

public class ParalelThreads{
    public static class Resultado{
        long totalNumeros;
        long suma; 
        int minimo; 
        int maximo;

        //Constructor
        public Resultado() {
            this.totalNumeros = 0;
            this.suma = 0;
            this.minimo = Integer.MAX_VALUE;
            this.maximo = Integer.MIN_VALUE;
        }

        public double getPromedio() {
            return totalNumeros == 0 ? 0.0 : (double) suma / totalNumeros;
        }

        public void acumular(int valor) {
            totalNumeros++;
            suma += valor;

            if (valor < minimo) {
                minimo = valor;
            }

            if (valor > maximo) {
                maximo = valor;
            }
        }

        public void combinar(Resultado otro) {
            // Si el resultado recibido es nulo o no contiene datos, no se combina.
            if (otro == null || otro.totalNumeros == 0) {
                return;
            }

            this.totalNumeros += otro.totalNumeros;
            this.suma += otro.suma;
            this.minimo = Math.min(this.minimo, otro.minimo);
            this.maximo = Math.max(this.maximo, otro.maximo);
        }

        @Override
        public String toString() {
            return "Total=" + totalNumeros +
                   ", Suma=" + suma +
                   ", Promedio=" + getPromedio() +
                   ", Min=" + minimo +
                   ", Max=" + maximo;
        }
    }

    //Clase trabajadora que procesa un bloque especifico del arreglo 
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
                res.acumular(datos[i]);
            }
            this.resultadoParcial = res;
        }

        public Resultado getResultadoParcial() {
            return resultadoParcial;
        }
    }

    public static int[] generarDatos(int n, int minValor, int maxValor) {
        int[] datos = new int[n];
        Random random = new Random();
        int rango = maxValor - minValor + 1;

        for (int i = 0; i < n; i++) {
            datos[i] = minValor + random.nextInt(rango);
        }

        return datos;
    }

    public static Resultado procesarSecuencial(int[] datos) {
        Resultado res = new Resultado();
        for (int valor : datos) {
            res.acumular(valor);
        }
        return res;
    }

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
            total.combinar(worker.getResultadoParcial());
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

    public static boolean mismosResultados(Resultado a, Resultado b) {
        return a.totalNumeros == b.totalNumeros
                && a.suma == b.suma
                && a.minimo == b.minimo
                && a.maximo == b.maximo;
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

        System.out.println("Generando " + n + " números aleatorios entre " + minValor + " y " + maxValor + "...");
        int[] datos = generarDatos(n, minValor, maxValor);

        long inicioSec = System.nanoTime();
        Resultado resultadoSec = procesarSecuencial(datos);
        long finSec = System.nanoTime();
        double tiempoSecMs = (finSec - inicioSec) / 1_000_000.0;

        System.out.println("\n=== RESULTADO SECUENCIAL ===");
        System.out.println(resultadoSec);
        System.out.printf("Tiempo secuencial: %.3f ms%n", tiempoSecMs);

        System.out.println("\n=== RESULTADOS PARALELOS ===");
        System.out.println("Hilos\tTiempo(ms)\tSpeedup\t\tCoincide con secuencial");

        for (int h : configuracionHilos) {
            try {
                long inicioPar = System.nanoTime();
                Resultado resultadoPar = procesarParalelo(datos, h);
                long finPar = System.nanoTime();

                double tiempoParMs = (finPar - inicioPar) / 1_000_000.0;
                double speedup = tiempoSecMs / tiempoParMs;
                boolean coincide = mismosResultados(resultadoSec, resultadoPar);

                System.out.printf("%d\t%.3f\t\t%.3f\t\t%s%n", h, tiempoParMs, speedup, coincide ? "Sí" : "No");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Se interrumpió la ejecución con " + h + " hilos.");
            } catch (IllegalArgumentException e) {
                System.out.println("Configuración inválida de hilos (" + h + "): " + e.getMessage());
            }
        }

        System.out.println("\nUso:");
        System.out.println("java EstadisticasParalelas [cantidad_datos] [lista_hilos]");
        System.out.println("Ejemplo:");
        System.out.println("java EstadisticasParalelas 1000000 2,4,8");
        System.out.println("Hilos probados: " + Arrays.toString(configuracionHilos));
    }
}