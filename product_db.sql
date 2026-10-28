/******************************
-  Nombre:      1.- Creación de base de datos
-  Descripción: Definición y creación del contenedor principal
               para el sistema de gestión de productos.
-  Tipo:        Estructura (Database)
-  Versión:     1.0
-  Autor:       Stalin Ladino
-  Fecha:       13-04-2026
*******************************/
CREATE DATABASE products_db;


/******************************
-  Nombre:      2.- Creación de tabla de productos
-  Descripción: Definición y creación de la tabla principal
               que almacena el catálogo de productos con
               control de precios y stock disponible.
-  Tipo:        Estructura (Table)
-  Versión:     1.0
-  Autor:       Stalin Ladino
-  Fecha:       13-04-2026
*******************************/
CREATE TABLE public.products (
    id      serial         NOT NULL,
    nombre  varchar(255)   NOT NULL,
    precio  numeric(38, 2) NOT NULL,
    stock   int4           NOT NULL,
    CONSTRAINT products_pkey       PRIMARY KEY (id),
    CONSTRAINT products_stock_check CHECK ((stock >= 0))
);